package com.cacaoscan.domain.model

import kotlin.math.roundToInt

/**
 * Nivel de confianza del modelo sobre la categoria elegida, en el rango [0, 1].
 *
 * Se modela como tipo propio y no como un Float suelto para evitar el error mas
 * frecuente al presentarlo: mezclar fracciones con porcentajes. Ademas deja
 * explicito en el dominio que esto es *confianza del modelo*, no probabilidad de
 * que el diagnostico sea correcto en campo.
 */
@JvmInline
value class Confidence private constructor(val fraction: Float) : Comparable<Confidence> {

    /** Valor entero para mostrar en pantalla, entre 0 y 100. */
    val percent: Int get() = (fraction * 100f).roundToInt().coerceIn(0, 100)

    override fun compareTo(other: Confidence): Int = fraction.compareTo(other.fraction)

    override fun toString(): String = "$percent%"

    companion object {
        /**
         * Crea una confianza a partir de una fraccion. Los valores fuera de
         * rango se recortan en lugar de lanzar: la salida de un modelo puede
         * traer pequenos desbordes numericos y no debe tumbar la aplicacion.
         */
        fun ofFraction(fraction: Float): Confidence =
            Confidence(if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f))
    }
}
