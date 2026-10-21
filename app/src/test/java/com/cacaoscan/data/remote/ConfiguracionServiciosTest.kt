package com.cacaoscan.data.remote

import com.cacaoscan.data.remote.ConfiguracionServicios.Companion.normalizarAnfitrion
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.security.credentials.KeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfiguracionServiciosTest {

    @Test
    fun `acepta direcciones IPv4 y nombres de equipo`() {
        assertEquals("10.0.2.2", normalizarAnfitrion("10.0.2.2"))
        assertEquals("192.168.1.20", normalizarAnfitrion("  192.168.1.20  "))
        assertEquals("portatil-esteban.local", normalizarAnfitrion("Portatil-Esteban.local"))
    }

    @Test
    fun `si se pega la URL completa del navegador se queda con el anfitrion`() {
        assertEquals("192.168.1.20", normalizarAnfitrion("http://192.168.1.20:8001/docs"))
        assertEquals("10.0.2.2", normalizarAnfitrion("https://10.0.2.2/"))
    }

    @Test
    fun `rechaza lo que no es una direccion`() {
        assertNull(normalizarAnfitrion(""))
        assertNull(normalizarAnfitrion("   "))
        assertNull(normalizarAnfitrion("192.168.1"))
        assertNull(normalizarAnfitrion("300.1.1.1"))
        assertNull(normalizarAnfitrion("mi equipo"))
    }

    @Test
    fun `por omision apunta al anfitrion del emulador`() {
        val config = ConfiguracionServicios(AlmacenEnMemoria())
        assertEquals("http://10.0.2.2:8001", config.urlBase(ServicioRemoto.REGISTRO_MODELOS))
        assertEquals("http://10.0.2.2:8002", config.urlBase(ServicioRemoto.CONTROL_CALIDAD))
    }

    @Test
    fun `una direccion invalida no sustituye a la guardada`() {
        val config = ConfiguracionServicios(AlmacenEnMemoria())
        config.cambiarAnfitrion("192.168.1.20")

        assertNull(config.cambiarAnfitrion("no valida"))
        assertEquals("192.168.1.20", config.anfitrion)
    }

    private class AlmacenEnMemoria : KeyValueStore {
        private val datos = mutableMapOf<String, String>()
        override fun getString(key: String): String? = datos[key]
        override fun putString(key: String, value: String?) {
            if (value == null) datos.remove(key) else datos[key] = value
        }
    }
}
