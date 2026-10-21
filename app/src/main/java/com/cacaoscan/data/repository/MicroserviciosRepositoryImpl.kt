package com.cacaoscan.data.repository

import com.cacaoscan.data.remote.ConfiguracionServicios
import com.cacaoscan.data.remote.LectorJson
import com.cacaoscan.data.remote.MonitorConectividad
import com.cacaoscan.data.remote.RespuestaHttp
import com.cacaoscan.data.remote.TransporteHttp
import com.cacaoscan.domain.model.admin.EstadoServicio
import com.cacaoscan.domain.model.admin.ImagenCandidata
import com.cacaoscan.domain.model.admin.MetricasServicio
import com.cacaoscan.domain.model.admin.ModeloRegistrado
import com.cacaoscan.domain.model.admin.MotivoSinConexion
import com.cacaoscan.domain.model.admin.NuevoModelo
import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.domain.model.admin.ResumenCalidad
import com.cacaoscan.domain.model.admin.ResumenRegistro
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.domain.model.admin.ServicioRemoto.CONTROL_CALIDAD
import com.cacaoscan.domain.model.admin.ServicioRemoto.REGISTRO_MODELOS
import com.cacaoscan.domain.model.admin.ValidacionImagen
import com.cacaoscan.domain.repository.MicroserviciosRepository
import org.json.JSONException
import java.io.IOException
import java.net.URLEncoder
import java.net.SocketTimeoutException

/**
 * Implementacion del acceso a los microservicios.
 *
 * Su trabajo es convertir cada forma posible de fallo en un [ResultadoRemoto]
 * concreto, para que ninguna excepcion de red llegue a la interfaz. Las reglas
 * de negocio NO se repiten aqui: si un modelo o una imagen no cumplen, lo dice
 * el validador del microservicio y la app solo muestra sus motivos. Duplicarlas
 * haria que las dos copias acabaran discrepando.
 */
class MicroserviciosRepositoryImpl(
    private val transporte: TransporteHttp,
    private val configuracion: ConfiguracionServicios,
    private val conectividad: MonitorConectividad,
) : MicroserviciosRepository {

    override val anfitrion: String get() = configuracion.anfitrion

    override fun cambiarAnfitrion(entrada: String): String? =
        configuracion.cambiarAnfitrion(entrada)

    // ------------------------------------------------------------- comunes

    override suspend fun estado(servicio: ServicioRemoto): EstadoServicio {
        val salud = get(servicio, "/salud") { LectorJson.saludViva(it) }
        if (salud !is ResultadoRemoto.Exito || !salud.datos) {
            val motivo = (salud as? ResultadoRemoto.SinConexion)?.motivo
                ?: MotivoSinConexion.SERVICIO_NO_RESPONDE
            return EstadoServicio(servicio, vivo = false, motivo = motivo)
        }
        val version = (get(servicio, "/version") { LectorJson.version(it) } as? ResultadoRemoto.Exito)?.datos
        return EstadoServicio(servicio, vivo = true, version = version)
    }

    override suspend fun metricas(servicio: ServicioRemoto): ResultadoRemoto<MetricasServicio> =
        get(servicio, "/metricas", LectorJson::metricas)

    // ------------------------------------------------- registro de modelos

    override suspend fun listarModelos(): ResultadoRemoto<List<ModeloRegistrado>> =
        get(REGISTRO_MODELOS, "/modelos", LectorJson::modelos)

    override suspend fun obtenerModelo(version: String): ResultadoRemoto<ModeloRegistrado> =
        get(REGISTRO_MODELOS, "/modelos/" + URLEncoder.encode(version, "UTF-8"), LectorJson::modelo)

    override suspend fun resumenRegistro(): ResultadoRemoto<ResumenRegistro> =
        get(REGISTRO_MODELOS, "/resumen", LectorJson::resumenRegistro)

    override suspend fun registrarModelo(modelo: NuevoModelo): ResultadoRemoto<ModeloRegistrado> =
        post(REGISTRO_MODELOS, "/modelos", LectorJson.nuevoModelo(modelo), LectorJson::modelo)

    // --------------------------------------------------- control de calidad

    override suspend fun validarImagen(imagen: ImagenCandidata): ResultadoRemoto<ValidacionImagen> =
        post(CONTROL_CALIDAD, "/validaciones", LectorJson.imagenCandidata(imagen), LectorJson::validacion)

    override suspend fun historialValidaciones(
        soloRechazadas: Boolean,
    ): ResultadoRemoto<List<ValidacionImagen>> =
        get(
            CONTROL_CALIDAD,
            if (soloRechazadas) "/validaciones/rechazadas" else "/validaciones",
            LectorJson::validaciones,
        )

    override suspend fun resumenCalidad(): ResultadoRemoto<ResumenCalidad> =
        get(CONTROL_CALIDAD, "/resumen", LectorJson::resumenCalidad)

    // ----------------------------------------------------------- transporte

    private suspend fun <T> get(
        servicio: ServicioRemoto,
        ruta: String,
        leer: (String) -> T,
    ): ResultadoRemoto<T> = llamar(leer) {
        transporte.get(configuracion.urlBase(servicio) + ruta)
    }

    private suspend fun <T> post(
        servicio: ServicioRemoto,
        ruta: String,
        cuerpo: String,
        leer: (String) -> T,
    ): ResultadoRemoto<T> = llamar(leer) {
        transporte.post(configuracion.urlBase(servicio) + ruta, cuerpo)
    }

    private suspend fun <T> llamar(
        leer: (String) -> T,
        peticion: suspend () -> RespuestaHttp,
    ): ResultadoRemoto<T> {
        // Se comprueba antes de intentar: sin red, el fallo de conexion tardaria
        // varios segundos y acabaria con un mensaje que culpa al servicio.
        if (!conectividad.hayRed()) {
            return ResultadoRemoto.SinConexion(MotivoSinConexion.TELEFONO_SIN_RED)
        }

        val respuesta = try {
            peticion()
        } catch (_: SocketTimeoutException) {
            return ResultadoRemoto.SinConexion(MotivoSinConexion.TIEMPO_AGOTADO)
        } catch (_: IOException) {
            return ResultadoRemoto.SinConexion(MotivoSinConexion.SERVICIO_NO_RESPONDE)
        }

        return when (respuesta.codigo) {
            in 200..299 -> try {
                ResultadoRemoto.Exito(leer(respuesta.cuerpo))
            } catch (_: JSONException) {
                ResultadoRemoto.SinConexion(MotivoSinConexion.RESPUESTA_ILEGIBLE)
            }

            422 -> ResultadoRemoto.Rechazado(
                LectorJson.mensajesDeError(respuesta.cuerpo)
                    .ifEmpty { listOf("El servicio rechazo los datos sin indicar el motivo.") },
            )

            else -> ResultadoRemoto.ErrorServicio(
                codigo = respuesta.codigo,
                mensaje = LectorJson.mensajesDeError(respuesta.cuerpo).firstOrNull()
                    ?: "El servicio respondio con el codigo ${respuesta.codigo}.",
            )
        }
    }
}
