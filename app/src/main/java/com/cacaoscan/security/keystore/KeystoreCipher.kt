package com.cacaoscan.security.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.cacaoscan.security.credentials.ByteCipher
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifrado simetrico AES/GCM con la clave guardada en el Android Keystore.
 *
 * Se usa para proteger el registro de la contrasena de administrador. Podria
 * parecer redundante, porque lo que se guarda ya es un hash PBKDF2 y no la
 * contrasena: la razon es que anade una barrera real frente a un ataque
 * offline. Si alguien extrae el archivo de preferencias de un telefono con
 * root, sin esta capa podria lanzar un ataque de diccionario contra el hash en
 * su propio equipo. Con ella necesita ademas la clave del Keystore, que en
 * dispositivos con hardware seguro no es extraible.
 *
 * GCM es un modo autenticado: si alguien modifica un solo byte del contenido
 * cifrado, el descifrado falla en lugar de devolver datos corruptos.
 */
class KeystoreCipher(private val alias: String) : ByteCipher {

    /** Devuelve IV || texto cifrado. El IV es publico y debe ser unico por cifrado. */
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain)
        return cipher.iv + encrypted
    }

    /** Recibe IV || texto cifrado. Lanza si el contenido fue alterado. */
    override fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_BYTES) { "Contenido cifrado incompleto." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES),
        )
        return cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return (keyStore.getKey(alias, null) as? SecretKey) ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
