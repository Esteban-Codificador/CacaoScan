package com.cacaoscan.data.repository

import com.cacaoscan.data.remote.ConfiguracionServicios
import com.cacaoscan.data.remote.RespuestaHttp
import com.cacaoscan.data.remote.TransporteHttp
import com.cacaoscan.domain.model.admin.MotivoSinConexion
import com.cacaoscan.domain.model.admin.NuevoModelo
import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.security.credentials.KeyValueStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Comprueba que cada forma de fallo de red se convierte en un resultado
 * concreto y que ninguna excepcion llega a la interfaz.
 */
class MicroserviciosRepositoryImplTest {

    private val transporte = TransporteFalso()
    private var hayRed = true
    private val repositorio = MicroserviciosRepositoryImpl(
        transporte = transporte,
        configuracion = ConfiguracionServicios(AlmacenEnMemoria()),
        conectividad = { hayRed },
    )

    @Test
    fun `sin red no se intenta ninguna peticion`() = runTest {
        hayRed = false

        val r = repositorio.listarModelos()

        assertEquals(ResultadoRemoto.SinConexion(MotivoSinConexion.TELEFONO_SIN_RED), r)
        assertTrue(transporte.urls.isEmpty())
    }

    @Test
    fun `cada servicio se consulta en su propio puerto`() = runTest {
        transporte.responder(200, "[]")
        repositorio.listarModelos()
        repositorio.historialValidaciones(soloRechazadas = true)

        assertEquals(
            listOf("http://10.0.2.2:8001/modelos", "http://10.0.2.2:8002/validaciones/rechazadas"),
            transporte.urls,
        )
    }

    @Test
    fun `servicio apagado se informa como que no responde`() = runTest {
        transporte.fallar(ConnectException("Connection refused"))
        assertEquals(
            ResultadoRemoto.SinConexion(MotivoSinConexion.SERVICIO_NO_RESPONDE),
            repositorio.resumenCalidad(),
        )
    }

    @Test
    fun `tiempo agotado se distingue de servicio apagado`() = runTest {
        transporte.fallar(SocketTimeoutException())
        assertEquals(
            ResultadoRemoto.SinConexion(MotivoSinConexion.TIEMPO_AGOTADO),
            repositorio.resumenCalidad(),
        )
    }

    @Test
    fun `un 422 del validador llega como rechazo con todos sus motivos`() = runTest {
        transporte.responder(
            422,
            """{"detail":{"mensaje":"No supero la validacion.","errores":["version mal","hash mal"]}}""",
        )

        val r = repositorio.registrarModelo(NuevoModelo("1.0", 1.4, 0.9, "abc", 250.0))

        assertEquals(ResultadoRemoto.Rechazado(listOf("version mal", "hash mal")), r)
    }

    @Test
    fun `un 404 llega como error del servicio con su mensaje`() = runTest {
        transporte.responder(404, """{"detail":"No existe la version '9.9.9'."}""")

        val r = repositorio.obtenerModelo("9.9.9")

        assertEquals(ResultadoRemoto.ErrorServicio(404, "No existe la version '9.9.9'."), r)
    }

    @Test
    fun `una respuesta 200 con formato inesperado no muestra datos a medias`() = runTest {
        transporte.responder(200, """{"otra":"cosa"}""")
        assertEquals(
            ResultadoRemoto.SinConexion(MotivoSinConexion.RESPUESTA_ILEGIBLE),
            repositorio.resumenRegistro(),
        )
    }

    @Test
    fun `estado de un servicio vivo incluye su version`() = runTest {
        transporte.responderSegunRuta(
            "/salud" to """{"estado":"vivo","servicio":"control-de-calidad"}""",
            "/version" to """{"servicio":"control-de-calidad","version":"1.0.0"}""",
        )

        val estado = repositorio.estado(ServicioRemoto.CONTROL_CALIDAD)

        assertTrue(estado.vivo)
        assertEquals("1.0.0", estado.version)
    }

    @Test
    fun `estado de un servicio caido dice por que`() = runTest {
        transporte.fallar(ConnectException())

        val estado = repositorio.estado(ServicioRemoto.REGISTRO_MODELOS)

        assertFalse(estado.vivo)
        assertEquals(MotivoSinConexion.SERVICIO_NO_RESPONDE, estado.motivo)
    }

    // ------------------------------------------------------------- dobles

    private class TransporteFalso : TransporteHttp {
        val urls = mutableListOf<String>()
        private var respuesta: (String) -> RespuestaHttp = { RespuestaHttp(200, "{}") }

        fun responder(codigo: Int, cuerpo: String) {
            respuesta = { RespuestaHttp(codigo, cuerpo) }
        }

        fun responderSegunRuta(vararg rutas: Pair<String, String>) {
            respuesta = { url ->
                val cuerpo = rutas.first { (ruta, _) -> url.endsWith(ruta) }.second
                RespuestaHttp(200, cuerpo)
            }
        }

        fun fallar(error: IOException) {
            respuesta = { throw error }
        }

        override suspend fun get(url: String): RespuestaHttp {
            urls += url
            return respuesta(url)
        }

        override suspend fun post(url: String, cuerpoJson: String): RespuestaHttp {
            urls += url
            return respuesta(url)
        }
    }

    private class AlmacenEnMemoria : KeyValueStore {
        private val datos = mutableMapOf<String, String>()
        override fun getString(key: String): String? = datos[key]
        override fun putString(key: String, value: String?) {
            if (value == null) datos.remove(key) else datos[key] = value
        }
    }
}
