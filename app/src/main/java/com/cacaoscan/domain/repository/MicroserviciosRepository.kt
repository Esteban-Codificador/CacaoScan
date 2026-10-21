package com.cacaoscan.domain.repository

import com.cacaoscan.domain.model.admin.EstadoServicio
import com.cacaoscan.domain.model.admin.ImagenCandidata
import com.cacaoscan.domain.model.admin.MetricasServicio
import com.cacaoscan.domain.model.admin.ModeloRegistrado
import com.cacaoscan.domain.model.admin.NuevoModelo
import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.domain.model.admin.ResumenCalidad
import com.cacaoscan.domain.model.admin.ResumenRegistro
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.domain.model.admin.ValidacionImagen

/**
 * Acceso a los dos microservicios del proyecto.
 *
 * Es la UNICA puerta de la aplicacion hacia la red. Solo la usan los ViewModels
 * de presentation.admin; el flujo del productor (escaneo, resultado e
 * historial) depende de ScanRepository, que es completamente local.
 */
interface MicroserviciosRepository {

    /** Direccion configurada del equipo que ejecuta los servicios. */
    val anfitrion: String

    /** Guarda una nueva direccion. Devuelve la normalizada, o null si no es valida. */
    fun cambiarAnfitrion(entrada: String): String?

    // Comunes a ambos servicios: etapas Operate y Monitor.
    suspend fun estado(servicio: ServicioRemoto): EstadoServicio
    suspend fun metricas(servicio: ServicioRemoto): ResultadoRemoto<MetricasServicio>

    // Microservicio 1: Registro de Modelos.
    suspend fun listarModelos(): ResultadoRemoto<List<ModeloRegistrado>>
    suspend fun obtenerModelo(version: String): ResultadoRemoto<ModeloRegistrado>
    suspend fun resumenRegistro(): ResultadoRemoto<ResumenRegistro>
    suspend fun registrarModelo(modelo: NuevoModelo): ResultadoRemoto<ModeloRegistrado>

    // Microservicio 2: Control de Calidad de Imagenes.
    suspend fun validarImagen(imagen: ImagenCandidata): ResultadoRemoto<ValidacionImagen>
    suspend fun historialValidaciones(soloRechazadas: Boolean = false): ResultadoRemoto<List<ValidacionImagen>>
    suspend fun resumenCalidad(): ResultadoRemoto<ResumenCalidad>
}
