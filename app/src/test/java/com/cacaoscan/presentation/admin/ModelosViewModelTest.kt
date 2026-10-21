package com.cacaoscan.presentation.admin

import com.cacaoscan.presentation.admin.ModelosViewModel.Companion.decimal
import com.cacaoscan.presentation.admin.ModelosViewModel.Companion.siguienteVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelosViewModelTest {

    @Test
    fun `sin modelos la primera version es 0_1_0`() {
        assertEquals("0.1.0", siguienteVersion(emptyList()))
    }

    @Test
    fun `sube el parche de la mayor version en orden semantico y no alfabetico`() {
        // Alfabeticamente "0.9.0" > "0.10.0"; semanticamente es al reves.
        assertEquals("0.10.1", siguienteVersion(listOf("0.9.0", "0.10.0", "0.2.5")))
    }

    @Test
    fun `ignora versiones mal formadas`() {
        assertEquals("1.0.1", siguienteVersion(listOf("1.0", "abc", "1.0.0")))
    }

    @Test
    fun `acepta coma decimal del teclado en espanol`() {
        assertEquals(0.912, decimal("0,912")!!, 0.0)
        assertEquals(4.7, decimal(" 4.7 ")!!, 0.0)
        assertNull(decimal("noventa"))
    }
}
