package com.cacaoscan.domain.model.admin

/** Resultado de consultar GET /salud y GET /version de un servicio. */
data class EstadoServicio(
    val servicio: ServicioRemoto,
    val vivo: Boolean,
    val version: String? = null,
    val motivo: MotivoSinConexion? = null,
)

/** Lectura de GET /metricas: lo que publica cada servicio para la etapa Monitor. */
data class MetricasServicio(
    val servicio: String,
    val version: String,
    val tiempoActivoSegundos: Double,
    val peticionesTotales: Int,
    val erroresTotales: Int,
    val tasaErrorPct: Double,
    val latenciaMedianaMs: Double,
    val latenciaP95Ms: Double,
    val latenciaMaximaMs: Double,
    val peticionesPorRuta: Map<String, Int>,
    val peticionesPorEstado: Map<String, Int>,
) {
    /** Mismo umbral que usa panel.py, para que la app y la consola digan lo mismo. */
    val degradado: Boolean get() = tasaErrorPct >= UMBRAL_DEGRADADO_PCT

    companion object {
        const val UMBRAL_DEGRADADO_PCT = 20.0
    }
}
