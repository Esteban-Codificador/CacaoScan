package com.cacaoscan.security.credentials

import com.cacaoscan.security.authz.constantTimeEquals
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Contrasena almacenada: sal, derivacion y numero de iteraciones.
 *
 * Se guarda el numero de iteraciones junto al hash para poder subirlo en el
 * futuro sin invalidar las contrasenas ya creadas.
 */
data class StoredPassword(
    val saltBase64: String,
    val hashBase64: String,
    val iterations: Int,
)

/**
 * Derivacion de contrasenas con PBKDF2-HMAC-SHA256.
 *
 * Por que PBKDF2 y no un hash directo como SHA-256:
 *
 *  - Un SHA-256 de una contrasena se rompe por fuerza bruta a millones de
 *    intentos por segundo. PBKDF2 aplica la funcion 100.000 veces, de modo que
 *    cada intento del atacante cuesta lo mismo que un inicio de sesion legitimo.
 *  - La SAL aleatoria hace que dos usuarios con la misma contrasena produzcan
 *    hashes distintos, e inutiliza las tablas precalculadas (rainbow tables).
 *  - La verificacion es de tiempo constante: comparar con salida temprana
 *    filtraria informacion sobre cuantos bytes del hash ya coinciden.
 *
 * La contrasena viaja como CharArray y no como String para poder borrarla de
 * memoria: un String en Java es inmutable y queda en el heap hasta que el
 * recolector decida liberarlo.
 *
 * Es codigo puro de JVM, sin dependencias de Android, y por eso esta cubierto
 * por pruebas unitarias (PasswordHasherTest).
 */
object PasswordHasher {

    /** Longitud minima exigida al crear la contrasena. */
    const val MIN_LENGTH = 8

    fun hash(
        password: CharArray,
        salt: ByteArray = newSalt(),
        iterations: Int = DEFAULT_ITERATIONS,
    ): StoredPassword {
        val derived = derive(password, salt, iterations)
        return StoredPassword(
            saltBase64 = encoder.encodeToString(salt),
            hashBase64 = encoder.encodeToString(derived),
            iterations = iterations,
        )
    }

    /** Verifica una contrasena contra lo almacenado, en tiempo constante. */
    fun verify(password: CharArray, stored: StoredPassword): Boolean {
        val salt = runCatching { decoder.decode(stored.saltBase64) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(stored.hashBase64) }.getOrNull() ?: return false
        val actual = derive(password, salt, stored.iterations)
        return constantTimeEquals(expected, actual)
    }

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            // Borra la copia interna que PBEKeySpec guarda de la contrasena.
            spec.clearPassword()
        }
    }

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /**
     * 100.000 iteraciones: suficientes para encarecer un ataque por fuerza bruta
     * y todavia por debajo de medio segundo en un dispositivo de gama media.
     */
    private const val DEFAULT_ITERATIONS = 100_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
}
