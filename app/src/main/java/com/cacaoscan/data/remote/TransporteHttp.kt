package com.cacaoscan.data.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta HTTP en bruto: codigo y cuerpo. La interpretacion la hace quien llama. */
data class RespuestaHttp(val codigo: Int, val cuerpo: String)

/**
 * Abstraccion minima del transporte HTTP.
 *
 * Existe para que el repositorio se pueda probar con un transporte falso, sin
 * levantar los microservicios ni un emulador. Lanza [IOException] cuando no se
 * puede llegar al servidor; cualquier codigo HTTP, incluidos 4xx y 5xx, es una
 * respuesta valida y se devuelve.
 */
interface TransporteHttp {
    suspend fun get(url: String): RespuestaHttp
    suspend fun post(url: String, cuerpoJson: String): RespuestaHttp
}

/**
 * Cliente HTTP sobre [HttpURLConnection], incluido en Android.
 *
 * Se usa en lugar de Retrofit u OkHttp de forma deliberada: el modulo de
 * administracion hace un punado de peticiones JSON a dos servicios conocidos, y
 * una biblioteca de red anadiria peso al APK (RNF08) sin aportar nada que aqui
 * haga falta. El JSON se lee con org.json, que tambien forma parte del sistema.
 */
class ClienteHttp(
    private val ioDispatcher: CoroutineDispatcher,
    private val tiempoConexionMs: Int = 3_000,
    private val tiempoLecturaMs: Int = 5_000,
) : TransporteHttp {

    override suspend fun get(url: String): RespuestaHttp = peticion(url, "GET", null)

    override suspend fun post(url: String, cuerpoJson: String): RespuestaHttp =
        peticion(url, "POST", cuerpoJson)

    private suspend fun peticion(url: String, metodo: String, cuerpo: String?): RespuestaHttp =
        withContext(ioDispatcher) {
            val conexion = URL(url).openConnection() as HttpURLConnection
            try {
                conexion.requestMethod = metodo
                conexion.connectTimeout = tiempoConexionMs
                conexion.readTimeout = tiempoLecturaMs
                conexion.setRequestProperty("Accept", "application/json")
                // Permite ver en los registros del servicio que la peticion vino de la app.
                conexion.setRequestProperty("User-Agent", "CacaoScan-Android-Admin")

                if (cuerpo != null) {
                    conexion.doOutput = true
                    conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
                }

                val codigo = conexion.responseCode
                // Para 4xx y 5xx el cuerpo viene por errorStream, no por inputStream.
                val flujo = if (codigo >= 400) conexion.errorStream else conexion.inputStream
                val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                RespuestaHttp(codigo, texto)
            } finally {
                conexion.disconnect()
            }
        }
}
