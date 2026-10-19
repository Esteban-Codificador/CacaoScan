package com.cacaoscan.security.authz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pruebas del modulo demostrativo de autorizacion.
 *
 * Se usa un firmante equivalente en la JVM en lugar del Android Keystore: lo que
 * se comprueba aqui es la logica del token (estructura, firma, expiracion y
 * scopes), no el almacenamiento de la clave, que es responsabilidad del sistema.
 */
class LocalTokenServiceTest {

    private val fixedInstant: Instant = Instant.parse("2026-09-05T12:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val signer = TestHmacSigner("clave-de-prueba")
    private val service = LocalTokenService(signer, clock)

    @Test
    fun `el token emitido tiene las tres partes de un JWT`() {
        val token = service.issue(setOf(AuthorizationScope.HISTORY_READ))
        assertEquals(3, token.split('.').size)
    }

    @Test
    fun `la cabecera declara el algoritmo y el tipo`() {
        val token = service.issue(setOf(AuthorizationScope.HISTORY_READ))
        val header = String(Base64.getUrlDecoder().decode(token.split('.')[0]))

        assertTrue(header.contains("\"alg\":\"HS256\""))
        assertTrue(header.contains("\"typ\":\"JWT\""))
    }

    @Test
    fun `un token recien emitido es valido y conserva sus claims`() {
        val token = service.issue(setOf(AuthorizationScope.HISTORY_READ))

        val verification = service.verify(token)

        assertTrue(verification is TokenVerification.Valid)
        val claims = (verification as TokenVerification.Valid).claims
        assertEquals(LocalTokenService.ISSUER, claims.issuer)
        assertEquals(LocalTokenService.SUBJECT, claims.subject)
        assertEquals(fixedInstant.epochSecond, claims.issuedAtEpochSeconds)
        assertEquals(setOf(AuthorizationScope.HISTORY_READ), claims.scopes)
    }

    @Test
    fun `autoriza solo los permisos incluidos en el token`() {
        val token = service.issue(setOf(AuthorizationScope.HISTORY_READ))

        assertTrue(service.verify(token, AuthorizationScope.HISTORY_READ) is TokenVerification.Valid)
        assertEquals(
            TokenVerification.MissingScope(AuthorizationScope.HISTORY_PURGE),
            service.verify(token, AuthorizationScope.HISTORY_PURGE),
        )
    }

    @Test
    fun `rechaza un token cuyo payload ha sido alterado`() {
        // Este es el punto central de JWT: modificar un claim invalida la firma.
        val token = service.issue(setOf(AuthorizationScope.HISTORY_READ))
        val parts = token.split('.')
        val tampered = JsonPayload(parts[1])
            .withScopes(listOf(AuthorizationScope.HISTORY_PURGE.claimValue))
            .encode()

        val result = service.verify("${parts[0]}.$tampered.${parts[2]}")

        assertEquals(TokenVerification.InvalidSignature, result)
    }

    @Test
    fun `rechaza un token firmado con otra clave`() {
        val otherService = LocalTokenService(TestHmacSigner("otra-clave"), clock)
        val foreignToken = otherService.issue(setOf(AuthorizationScope.HISTORY_READ))

        assertEquals(TokenVerification.InvalidSignature, service.verify(foreignToken))
    }

    @Test
    fun `rechaza un token expirado`() {
        val shortLived = LocalTokenService(signer, clock, Duration.ofMinutes(15))
        val token = shortLived.issue(setOf(AuthorizationScope.HISTORY_READ))

        val laterClock = Clock.fixed(fixedInstant.plus(Duration.ofMinutes(16)), ZoneOffset.UTC)
        val laterService = LocalTokenService(signer, laterClock)

        assertEquals(TokenVerification.Expired, laterService.verify(token))
    }

    @Test
    fun `rechaza cadenas que no son tokens`() {
        assertEquals(TokenVerification.Malformed, service.verify("esto-no-es-un-token"))
        assertEquals(TokenVerification.Malformed, service.verify("a.b"))
        assertEquals(TokenVerification.Malformed, service.verify(""))
    }

    @Test
    fun `dos claves distintas producen firmas distintas para el mismo contenido`() {
        val content = "contenido".toByteArray()
        assertNotEquals(
            Base64.getEncoder().encodeToString(TestHmacSigner("a").sign(content)),
            Base64.getEncoder().encodeToString(TestHmacSigner("b").sign(content)),
        )
    }

    /** Firmante HMAC-SHA256 equivalente al del Keystore, pero ejecutable en la JVM. */
    private class TestHmacSigner(secret: String) : TokenSigner {
        private val key = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        override val algorithm: String = "HS256"

        override fun sign(content: ByteArray): ByteArray =
            Mac.getInstance("HmacSHA256").apply { init(key) }.doFinal(content)

        override fun verify(content: ByteArray, signature: ByteArray): Boolean =
            constantTimeEquals(sign(content), signature)
    }

    /** Utilidad minima para alterar el payload de un token en las pruebas. */
    private class JsonPayload(encoded: String) {
        private var json = String(Base64.getUrlDecoder().decode(encoded))

        fun withScopes(scopes: List<String>): JsonPayload {
            val replacement = scopes.joinToString(",") { "\"$it\"" }
            json = json.replace(Regex("\"scope\":\\[[^]]*]"), "\"scope\":[$replacement]")
            return this
        }

        fun encode(): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray())
    }
}
