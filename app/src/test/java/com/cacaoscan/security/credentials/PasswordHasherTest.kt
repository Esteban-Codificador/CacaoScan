package com.cacaoscan.security.credentials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `acepta la contrasena correcta`() {
        val stored = PasswordHasher.hash("cacao2026".toCharArray())
        assertTrue(PasswordHasher.verify("cacao2026".toCharArray(), stored))
    }

    @Test
    fun `rechaza una contrasena incorrecta`() {
        val stored = PasswordHasher.hash("cacao2026".toCharArray())
        assertFalse(PasswordHasher.verify("cacao2025".toCharArray(), stored))
        assertFalse(PasswordHasher.verify("".toCharArray(), stored))
    }

    @Test
    fun `la misma contrasena produce hashes distintos por la sal aleatoria`() {
        // Es lo que inutiliza las tablas precalculadas: dos instalaciones con la
        // misma contrasena no comparten hash.
        val uno = PasswordHasher.hash("cacao2026".toCharArray())
        val dos = PasswordHasher.hash("cacao2026".toCharArray())

        assertNotEquals(uno.saltBase64, dos.saltBase64)
        assertNotEquals(uno.hashBase64, dos.hashBase64)
        assertTrue(PasswordHasher.verify("cacao2026".toCharArray(), uno))
        assertTrue(PasswordHasher.verify("cacao2026".toCharArray(), dos))
    }

    @Test
    fun `con la misma sal el resultado es reproducible`() {
        val salt = PasswordHasher.newSalt()
        val uno = PasswordHasher.hash("cacao2026".toCharArray(), salt)
        val dos = PasswordHasher.hash("cacao2026".toCharArray(), salt)

        assertEquals(uno.hashBase64, dos.hashBase64)
    }

    @Test
    fun `la contrasena nunca aparece en lo almacenado`() {
        val stored = PasswordHasher.hash("cacao2026".toCharArray())
        assertFalse(stored.hashBase64.contains("cacao"))
        assertFalse(stored.saltBase64.contains("cacao"))
    }

    @Test
    fun `un registro corrupto no hace fallar la verificacion`() {
        val corrupto = StoredPassword(
            saltBase64 = "no-es-base64-valido!!",
            hashBase64 = "tampoco",
            iterations = 1000,
        )
        assertFalse(PasswordHasher.verify("cacao2026".toCharArray(), corrupto))
    }

    @Test
    fun `distingue contrasenas que solo difieren en el ultimo caracter`() {
        val stored = PasswordHasher.hash("contrasena1".toCharArray())
        assertFalse(PasswordHasher.verify("contrasena2".toCharArray(), stored))
    }
}
