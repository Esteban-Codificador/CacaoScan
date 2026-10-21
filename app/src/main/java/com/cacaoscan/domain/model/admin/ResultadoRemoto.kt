package com.cacaoscan.domain.model.admin

/**
 * Resultado de una llamada a un microservicio.
 *
 * Se distinguen cuatro situaciones porque cada una pide una accion distinta a
 * quien administra: corregir los datos, revisar el servicio, revisar la red del
 * telefono o revisar la direccion configurada. Un simple "fallo" no diria cual.
 */
sealed interface ResultadoRemoto<out T> {

    data class Exito<T>(val datos: T) : ResultadoRemoto<T>

    /** El servicio respondio, pero rechazo el contenido por sus reglas de negocio (HTTP 422). */
    data class Rechazado(val errores: List<String>) : ResultadoRemoto<Nothing>

    /** El servicio respondio con un error que no es de validacion (404, 500...). */
    data class ErrorServicio(val codigo: Int, val mensaje: String) : ResultadoRemoto<Nothing>

    /** No se pudo llegar al servicio: sin red, direccion incorrecta o servicio apagado. */
    data class SinConexion(val motivo: MotivoSinConexion) : ResultadoRemoto<Nothing>
}

enum class MotivoSinConexion(val mensaje: String) {
    TELEFONO_SIN_RED(
        "El telefono no tiene ninguna red activa. Conectelo a la misma Wi-Fi que el equipo.",
    ),
    SERVICIO_NO_RESPONDE(
        "El servicio no responde. Compruebe que 'python operar.py' esta en marcha y que " +
            "la direccion configurada es la del equipo.",
    ),
    TIEMPO_AGOTADO(
        "El servicio tardo demasiado en responder.",
    ),
    RESPUESTA_ILEGIBLE(
        "El servicio respondio con un formato inesperado. Puede que la version no coincida.",
    ),
}
