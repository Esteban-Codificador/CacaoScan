package com.cacaoscan.security.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.cacaoscan.security.authz.TokenSigner
import com.cacaoscan.security.authz.constantTimeEquals
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Firma HMAC-SHA256 con una clave guardada en el Android Keystore.
 *
 * Por que el Keystore y no una constante en el codigo:
 *
 *  - La clave se genera en el dispositivo la primera vez que se necesita y NUNCA
 *    existe como literal en el codigo fuente ni en el APK. Un atacante que
 *    descompile la aplicacion no obtiene la clave.
 *  - El material de la clave no sale del Keystore: se envia el mensaje al
 *    sistema y se recibe la firma. En dispositivos con hardware seguro la clave
 *    vive en el elemento seguro y es inextraible incluso con root.
 *  - La clave es distinta en cada instalacion, de modo que un token de un
 *    telefono no vale en otro.
 *
 * LIMITE HONESTO: esto protege la clave, no convierte el esquema en
 * autenticacion federada. Emisor y verificador siguen siendo la misma
 * aplicacion. Ver /docs/06-seguridad-y-autorizacion.md
 */
class KeystoreHmacSigner : TokenSigner {

    override val algorithm: String = "HS256"

    override fun sign(content: ByteArray): ByteArray = mac().doFinal(content)

    override fun verify(content: ByteArray, signature: ByteArray): Boolean =
        constantTimeEquals(sign(content), signature)

    private fun mac(): Mac = Mac.getInstance(MAC_ALGORITHM).apply { init(secretKey()) }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        return existing ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            ).build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "cacaoscan_local_authz_hmac"
        const val MAC_ALGORITHM = "HmacSHA256"
    }
}
