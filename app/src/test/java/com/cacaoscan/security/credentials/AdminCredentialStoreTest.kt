package com.cacaoscan.security.credentials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AdminCredentialStoreTest {

    private val kv = FakeKeyValueStore()
    private val cipher = ReversibleTestCipher()
    private val clock = MutableClock(Instant.parse("2026-10-13T08:00:00Z"))

    private val store = DefaultAdminCredentialStore(kv, cipher, clock)

    @Test
    fun `al inicio no hay contrasena configurada`() {
        assertFalse(store.isConfigured())
        assertEquals(LoginResult.NotConfigured, store.verify("loquesea".toCharArray()))
    }

    @Test
    fun `tras crear la contrasena queda configurada y permite entrar`() {
        store.createPassword("cacao2026".toCharArray())

        assertTrue(store.isConfigured())
        assertEquals(LoginResult.Success, store.verify("cacao2026".toCharArray()))
    }

    @Test
    fun `rechaza contrasenas mas cortas que el minimo`() {
        val error = runCatching { store.createPassword("corta".toCharArray()) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertFalse(store.isConfigured())
    }

    @Test
    fun `lo guardado no contiene la contrasena en claro`() {
        store.createPassword("cacao2026".toCharArray())
        val guardado = kv.values.values.joinToString()
        assertFalse("La contrasena no debe poder leerse", guardado.contains("cacao2026"))
    }

    @Test
    fun `descuenta los intentos restantes en cada fallo`() {
        store.createPassword("cacao2026".toCharArray())

        assertEquals(LoginResult.Wrong(remainingAttempts = 4), store.verify("mala".toCharArray()))
        assertEquals(LoginResult.Wrong(remainingAttempts = 3), store.verify("mala".toCharArray()))
    }

    @Test
    fun `bloquea el acceso tras cinco fallos seguidos`() {
        store.createPassword("cacao2026".toCharArray())
        repeat(4) { store.verify("mala".toCharArray()) }

        val resultado = store.verify("mala".toCharArray())

        assertTrue(resultado is LoginResult.Locked)
        // Incluso con la contrasena correcta debe seguir bloqueado.
        assertTrue(store.verify("cacao2026".toCharArray()) is LoginResult.Locked)
    }

    @Test
    fun `el bloqueo sobrevive a un reinicio de la aplicacion`() {
        store.createPassword("cacao2026".toCharArray())
        repeat(5) { store.verify("mala".toCharArray()) }

        // Un atacante que reinicie la app encuentra el mismo bloqueo: el estado
        // vive en el almacenamiento, no en memoria.
        val trasReinicio = DefaultAdminCredentialStore(kv, cipher, clock)

        assertTrue(trasReinicio.lockedSecondsRemaining() > 0)
        assertTrue(trasReinicio.verify("cacao2026".toCharArray()) is LoginResult.Locked)
    }

    @Test
    fun `al expirar el bloqueo vuelve a aceptar la contrasena correcta`() {
        store.createPassword("cacao2026".toCharArray())
        repeat(5) { store.verify("mala".toCharArray()) }

        clock.advanceSeconds(61)

        assertEquals(0L, store.lockedSecondsRemaining())
        assertEquals(LoginResult.Success, store.verify("cacao2026".toCharArray()))
    }

    @Test
    fun `un acierto reinicia el contador de fallos`() {
        store.createPassword("cacao2026".toCharArray())
        repeat(3) { store.verify("mala".toCharArray()) }

        assertEquals(LoginResult.Success, store.verify("cacao2026".toCharArray()))
        assertEquals(LoginResult.Wrong(remainingAttempts = 4), store.verify("mala".toCharArray()))
    }

    @Test
    fun `un registro ilegible se trata como no configurado`() {
        kv.putString("admin_credential", "esto-no-descifra")
        assertFalse(store.isConfigured())
    }

    // --- dobles de prueba ---

    private class FakeKeyValueStore : KeyValueStore {
        val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String?) {
            if (value == null) values.remove(key) else values[key] = value
        }
    }

    /**
     * Cifrado de prueba reversible: invierte los bytes. No aporta seguridad,
     * solo sustituye al Keystore, que no existe en la JVM.
     */
    private class ReversibleTestCipher : ByteCipher {
        override fun encrypt(plain: ByteArray): ByteArray = plain.reversedArray()
        override fun decrypt(blob: ByteArray): ByteArray = blob.reversedArray()
    }

    private class MutableClock(private var instant: Instant) : Clock() {
        fun advanceSeconds(seconds: Long) {
            instant = instant.plusSeconds(seconds)
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = instant
    }
}
