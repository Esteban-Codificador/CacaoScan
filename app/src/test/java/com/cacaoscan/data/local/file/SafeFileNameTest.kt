package com.cacaoscan.data.local.file

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los nombres de archivo se generan internamente con UUID, pero se validan
 * igualmente: un registro corrupto en la base de datos no debe poder alcanzar
 * archivos fuera del directorio de la aplicacion.
 */
class SafeFileNameTest {

    @Test
    fun `acepta los nombres que genera la aplicacion`() {
        assertTrue(SafeFileName.isValid("3f0c1f2e-9a11-4d0b-9d3f-2a7c1b8e5d40.jpg"))
    }

    @Test
    fun `rechaza recorridos de ruta`() {
        assertFalse(SafeFileName.isValid("../../datos/secreto.txt"))
        assertFalse(SafeFileName.isValid(".."))
    }

    @Test
    fun `rechaza separadores de directorio`() {
        assertFalse(SafeFileName.isValid("subcarpeta/imagen.jpg"))
        assertFalse(SafeFileName.isValid("subcarpeta\\imagen.jpg"))
    }

    @Test
    fun `rechaza nombres vacios o solo con espacios`() {
        assertFalse(SafeFileName.isValid(""))
        assertFalse(SafeFileName.isValid("   "))
    }
}
