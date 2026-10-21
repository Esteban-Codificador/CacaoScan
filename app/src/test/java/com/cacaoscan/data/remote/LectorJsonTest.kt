package com.cacaoscan.data.remote

import com.cacaoscan.domain.model.admin.ImagenCandidata
import com.cacaoscan.domain.model.admin.NuevoModelo
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de contrato con los microservicios.
 *
 * Los JSON de entrada son respuestas REALES capturadas de los servicios en
 * ejecucion (curl contra 127.0.0.1:8001 y :8002), no ejemplos inventados. Si un
 * servicio cambia el nombre o el tipo de un campo, estas pruebas fallan antes
 * de que lo haga la aplicacion.
 */
class LectorJsonTest {

    @Test
    fun `lee las metricas de GET metricas`() {
        val json = """{"servicio":"registro-de-modelos","version":"1.0.0","tiempo_activo_segundos":7.8,""" +
            """"peticiones_totales":1,"errores_totales":0,"tasa_error_pct":0.0,""" +
            """"latencia_ms":{"mediana":3.25,"p95":3.25,"maxima":3.25},""" +
            """"peticiones_por_ruta":{"GET /salud":1},"peticiones_por_estado":{"200":1}}"""

        val m = LectorJson.metricas(json)

        assertEquals("registro-de-modelos", m.servicio)
        assertEquals("1.0.0", m.version)
        assertEquals(1, m.peticionesTotales)
        assertEquals(3.25, m.latenciaP95Ms, 0.0)
        assertEquals(mapOf("GET /salud" to 1), m.peticionesPorRuta)
        assertEquals(mapOf("200" to 1), m.peticionesPorEstado)
        assertFalse(m.degradado)
    }

    @Test
    fun `el resumen de un registro vacio trae nulos y no se confunden con cero`() {
        val json = """{"total_registrados":0,"aptos_para_publicar":0,"no_aptos":0,""" +
            """"mejor_exactitud":null,"version_vigente":null}"""

        val r = LectorJson.resumenRegistro(json)

        assertEquals(0, r.totalRegistrados)
        assertNull(r.mejorExactitud)
        assertNull(r.versionVigente)
    }

    @Test
    fun `lee un modelo registrado con los campos de servicio_py`() {
        val json = """{"version":"1.2.0","exactitud":0.912,"sensibilidad":0.934,""" +
            """"hash_sha256":"${"a".repeat(64)}","tamano_mb":4.7,"arquitectura":"MobileNetV3-Small",""" +
            """"notas":"","apto_para_publicar":true,"motivo":"Alcanza el 85 %",""" +
            """"registrado_en":"2026-10-20T23:20:04+00:00"}"""

        val lista = LectorJson.modelos("[$json]")

        assertEquals(1, lista.size)
        assertEquals("1.2.0", lista[0].version)
        assertTrue(lista[0].aptoParaPublicar)
        assertEquals(4.7, lista[0].tamanoMb, 0.0)
    }

    @Test
    fun `una imagen con vainas sanas y afectadas llega rechazada y sin clase binaria`() {
        val json = """{"archivo":"IMG_mixta.jpg","ancho":3024,"alto":3024,"peso_kb":2390.0,""" +
            """"clases":["h0","m2"],"clase_binaria":null,"aceptada":false,""" +
            """"errores":["Etiqueta ambigua: la imagen contiene vainas sanas y afectadas a la vez."],""" +
            """"advertencias":[],"validada_en":"2026-10-20T23:20:04+00:00"}"""

        val v = LectorJson.validacion(json)

        assertFalse(v.aceptada)
        assertNull(v.claseBinaria)
        assertEquals(listOf("h0", "m2"), v.clases)
        assertTrue(v.errores.single().startsWith("Etiqueta ambigua"))
    }

    @Test
    fun `lee el resumen de calidad con sus motivos de rechazo`() {
        val json = """{"total_validadas":1,"aceptadas":0,"rechazadas":1,"tasa_aceptacion_pct":0.0,""" +
            """"con_advertencias":0,"motivos_de_rechazo":{"Etiqueta ambigua":1},"aceptadas_por_clase":{}}"""

        val r = LectorJson.resumenCalidad(json)

        assertEquals(1, r.rechazadas)
        assertEquals(mapOf("Etiqueta ambigua" to 1), r.motivosDeRechazo)
        assertTrue(r.aceptadasPorClase.isEmpty())
    }

    // --------------------------------------------- las tres formas de error

    @Test
    fun `rechazo de negocio del Registro devuelve cada motivo del validador`() {
        val json = """{"detail":{"mensaje":"El modelo no supero la validacion.","errores":[""" +
            """"La version '1.0' no sigue el formato semantico X.Y.Z.",""" +
            """"El modelo pesa 250.0 MB y supera el limite de 100.0 MB (RNF08)."]}}"""

        val mensajes = LectorJson.mensajesDeError(json)

        assertEquals(2, mensajes.size)
        assertTrue(mensajes[1].contains("RNF08"))
    }

    @Test
    fun `error de esquema de Pydantic indica el campo que falta`() {
        val json = """{"detail":[{"type":"missing","loc":["body","exactitud"],"msg":"Field required",""" +
            """"input":{"version":"9.0.0"}},{"type":"missing","loc":["body","tamano_mb"],""" +
            """"msg":"Field required","input":{"version":"9.0.0"}}]}"""

        assertEquals(
            listOf("exactitud: Field required", "tamano_mb: Field required"),
            LectorJson.mensajesDeError(json),
        )
    }

    @Test
    fun `error simple de 404 devuelve el texto`() {
        assertEquals(
            listOf("No existe la version '9.9.9'."),
            LectorJson.mensajesDeError("""{"detail":"No existe la version '9.9.9'."}"""),
        )
    }

    @Test
    fun `un cuerpo de error que no es JSON no rompe nada`() {
        assertTrue(LectorJson.mensajesDeError("Internal Server Error").isEmpty())
    }

    // ------------------------------------------------- lo que envia la app

    @Test
    fun `el cuerpo de POST modelos usa los nombres de campo del servicio`() {
        val json = JSONObject(
            LectorJson.nuevoModelo(NuevoModelo("1.0.0", 0.9, 0.95, "f".repeat(64), 4.7)),
        )
        listOf("version", "exactitud", "sensibilidad", "hash_sha256", "tamano_mb", "arquitectura", "notas")
            .forEach { assertTrue("falta $it", json.has(it)) }
        assertEquals(0.9, json.getDouble("exactitud"), 0.0)
    }

    @Test
    fun `el cuerpo de POST validaciones envia las clases como lista`() {
        val json = JSONObject(
            LectorJson.imagenCandidata(ImagenCandidata("a.jpg", 300, 400, 12.5, listOf("h0"))),
        )
        assertEquals("h0", json.getJSONArray("clases").getString(0))
        assertEquals(12.5, json.getDouble("peso_kb"), 0.0)
    }
}
