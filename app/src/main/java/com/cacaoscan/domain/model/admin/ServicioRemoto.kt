package com.cacaoscan.domain.model.admin

/**
 * Los dos microservicios del proyecto que la aplicacion sabe consumir.
 *
 * Los puertos son los mismos que fija cada servicio en su script de endpoint
 * (servicio_modelos/endpoint.py y servicio_calidad/endpoint.py).
 */
enum class ServicioRemoto(
    val etiqueta: String,
    val descripcion: String,
    val puerto: Int,
) {
    REGISTRO_MODELOS(
        etiqueta = "Registro de Modelos",
        descripcion = "Versiona los modelos entrenados y decide cual es apto para publicar",
        puerto = 8001,
    ),
    CONTROL_CALIDAD(
        etiqueta = "Control de Calidad",
        descripcion = "Decide si una fotografia es apta para el conjunto de entrenamiento",
        puerto = 8002,
    ),
}
