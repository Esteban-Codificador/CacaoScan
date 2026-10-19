package com.cacaoscan.security.authz

/**
 * Firma y verifica bytes con una clave simetrica.
 *
 * Existe como interfaz para que la logica del token (construccion, codificacion,
 * expiracion, comprobacion de scopes) pueda probarse en la JVM sin dispositivo,
 * mientras que en produccion la clave nunca sale del Android Keystore.
 */
interface TokenSigner {

    /** Identificador del algoritmo, tal como aparece en la cabecera del token. */
    val algorithm: String

    /** Calcula la firma de [content]. */
    fun sign(content: ByteArray): ByteArray

    /**
     * Comprueba una firma.
     *
     * La comparacion debe ser de tiempo constante: comparar arreglos de bytes
     * con salida temprana filtra informacion sobre la firma correcta.
     */
    fun verify(content: ByteArray, signature: ByteArray): Boolean
}

/** Comparacion en tiempo constante, independiente del contenido. */
internal fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var difference = 0
    for (index in a.indices) {
        difference = difference or (a[index].toInt() xor b[index].toInt())
    }
    return difference == 0
}
