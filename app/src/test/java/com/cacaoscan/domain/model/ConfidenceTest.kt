package com.cacaoscan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfidenceTest {

    @Test
    fun `convierte la fraccion a porcentaje redondeado`() {
        assertEquals(83, Confidence.ofFraction(0.8271f).percent)
        assertEquals(50, Confidence.ofFraction(0.495f).percent)
        assertEquals(0, Confidence.ofFraction(0f).percent)
        assertEquals(100, Confidence.ofFraction(1f).percent)
    }

    @Test
    fun `recorta valores fuera de rango en lugar de fallar`() {
        // Las salidas de un modelo pueden traer pequenos desbordes numericos;
        // no deben tumbar la aplicacion en mitad de una revision.
        assertEquals(1f, Confidence.ofFraction(1.3f).fraction, 0.0001f)
        assertEquals(0f, Confidence.ofFraction(-0.2f).fraction, 0.0001f)
    }

    @Test
    fun `trata NaN como confianza nula`() {
        assertEquals(0f, Confidence.ofFraction(Float.NaN).fraction, 0.0001f)
    }

    @Test
    fun `se muestra siempre como porcentaje`() {
        assertEquals("64%", Confidence.ofFraction(0.6432f).toString())
    }
}
