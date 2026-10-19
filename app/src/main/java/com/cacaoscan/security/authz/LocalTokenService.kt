package com.cacaoscan.security.authz

import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.Duration
import java.util.Base64

/**
 * Claims del token local.
 */
data class TokenClaims(
    val issuer: String,
    val subject: String,
    val issuedAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val scopes: Set<AuthorizationScope>,
)

/** Resultado de verificar un token. */
sealed interface TokenVerification {
    data class Valid(val claims: TokenClaims) : TokenVerification
    data object Malformed : TokenVerification
    data object InvalidSignature : TokenVerification
    data object Expired : TokenVerification
    data class MissingScope(val required: AuthorizationScope) : TokenVerification
}

/**
 * MODULO DEMOSTRATIVO DE AUTORIZACION - ALCANCE ACADEMICO
 * =======================================================
 *
 * Emite y verifica un JSON Web Token (RFC 7519) firmado con HMAC-SHA256, con la
 * clave guardada en el Android Keystore. La estructura es la real:
 * cabecera.payload.firma, codificadas en Base64URL sin relleno.
 *
 * QUE DEMUESTRA ESTE MODULO
 *  - La estructura de un JWT y por que el payload es legible por cualquiera:
 *    va codificado en Base64, NO cifrado. Por eso aqui no se guarda ningun dato
 *    sensible dentro del token.
 *  - Verificacion de firma: si alguien altera un claim, la firma deja de
 *    coincidir y el token se rechaza.
 *  - Expiracion mediante el claim exp.
 *  - Control de acceso por scopes: una accion solo se permite si el token
 *    incluye el permiso correspondiente.
 *
 * QUE NO RESUELVE, Y ES IMPORTANTE DECIRLO
 *  - No hay confianza entre partes distintas. En un sistema real el valor de JWT
 *    esta en que un servidor emite el token y OTRO servicio lo verifica sin
 *    consultar al primero. Aqui emisor y verificador son la misma aplicacion, de
 *    modo que la firma no aporta garantia frente a terceros.
 *  - No hay revocacion: un token emitido es valido hasta que expira.
 *  - No protege frente a un atacante con acceso fisico y privilegios de root en
 *    el dispositivo.
 *  - No es autenticacion. Quien autentica es el sistema operativo mediante
 *    BiometricPrompt (ver com.cacaoscan.security.lock.AppLock); este
 *    modulo solo decide QUE puede hacerse una vez autenticado.
 *
 * POR QUE NO SE IMPLEMENTAN OTP, SSO NI OAuth 2.0: los tres exigen un servidor
 * de identidad o una pasarela de mensajeria, y el IEEE830 excluye del alcance el
 * uso de servidores y la gestion de cuentas de usuario, ademas de exigir
 * funcionamiento sin conexion. El analisis completo esta en
 * /docs/06-seguridad-y-autorizacion.md
 */
class LocalTokenService(
    private val signer: TokenSigner,
    private val clock: Clock,
    private val validity: Duration = DEFAULT_VALIDITY,
) {

    /** Emite un token con los permisos indicados. */
    fun issue(scopes: Set<AuthorizationScope>): String {
        val now = clock.instant().epochSecond
        val header = JSONObject()
            .put("alg", signer.algorithm)
            .put("typ", "JWT")

        val payload = JSONObject()
            .put("iss", ISSUER)
            .put("sub", SUBJECT)
            .put("iat", now)
            .put("exp", now + validity.seconds)
            .put("scope", JSONArray(scopes.map { it.claimValue }))

        val signingInput = "${encode(header.toString())}.${encode(payload.toString())}"
        val signature = signer.sign(signingInput.toByteArray(Charsets.UTF_8))
        return "$signingInput.${encode(signature)}"
    }

    /**
     * Verifica firma, vigencia y, si se indica, la presencia de un permiso.
     *
     * El orden importa: primero la firma y despues los claims. Interpretar
     * claims de un token cuya firma no se ha comprobado es el error clasico al
     * usar JWT.
     */
    fun verify(token: String, requiredScope: AuthorizationScope? = null): TokenVerification {
        val parts = token.split('.')
        if (parts.size != 3) return TokenVerification.Malformed

        val signingInput = "${parts[0]}.${parts[1]}"
        val signature = runCatching { decode(parts[2]) }.getOrNull()
            ?: return TokenVerification.Malformed

        if (!signer.verify(signingInput.toByteArray(Charsets.UTF_8), signature)) {
            return TokenVerification.InvalidSignature
        }

        val claims = runCatching { parseClaims(parts[1]) }.getOrNull()
            ?: return TokenVerification.Malformed

        if (clock.instant().epochSecond >= claims.expiresAtEpochSeconds) {
            return TokenVerification.Expired
        }

        if (requiredScope != null && requiredScope !in claims.scopes) {
            return TokenVerification.MissingScope(requiredScope)
        }

        return TokenVerification.Valid(claims)
    }

    private fun parseClaims(encodedPayload: String): TokenClaims {
        val json = JSONObject(String(decode(encodedPayload), Charsets.UTF_8))
        val scopeArray = json.optJSONArray("scope") ?: JSONArray()
        val scopes = buildSet {
            for (index in 0 until scopeArray.length()) {
                AuthorizationScope.fromClaim(scopeArray.getString(index))?.let(::add)
            }
        }
        return TokenClaims(
            issuer = json.getString("iss"),
            subject = json.getString("sub"),
            issuedAtEpochSeconds = json.getLong("iat"),
            expiresAtEpochSeconds = json.getLong("exp"),
            scopes = scopes,
        )
    }

    private fun encode(value: String): String = encode(value.toByteArray(Charsets.UTF_8))

    private fun encode(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun decode(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

    companion object {
        const val ISSUER = "cacaoscan-local"

        /**
         * No hay cuentas de usuario en CacaoScan: el sujeto es siempre el propio
         * dispositivo. Nombrarlo asi deja claro que no se esta modelando una
         * identidad de persona, que estaria fuera del alcance del proyecto.
         */
        const val SUBJECT = "dispositivo-local"

        /**
         * Vigencia corta a proposito: una sesion autorizada no debe quedar
         * abierta indefinidamente si el telefono cambia de manos.
         */
        val DEFAULT_VALIDITY: Duration = Duration.ofMinutes(15)
    }
}
