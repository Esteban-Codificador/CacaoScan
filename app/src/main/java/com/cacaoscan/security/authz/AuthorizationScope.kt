package com.cacaoscan.security.authz

/**
 * Permisos que puede otorgar un token local.
 *
 * Deliberadamente NO existe un scope para escanear o clasificar: el flujo
 * principal de CacaoScan (RF01 a RF05) nunca depende de este modulo. Solo se
 * protegen acciones secundarias, destructivas o que salen del telefono.
 */
enum class AuthorizationScope(val claimValue: String) {
    /** Consultar el historial. */
    HISTORY_READ("history:read"),

    /** Borrar una revision concreta. */
    HISTORY_DELETE("history:delete"),

    /** Borrar todo el historial de una vez. Accion irreversible. */
    HISTORY_PURGE("history:purge"),

    /**
     * Consultar los microservicios: estado, metricas, modelos y validaciones.
     * Lo exigen todas las pantallas del modulo de administracion.
     */
    SERVICES_READ("servicios:leer"),

    /**
     * Escribir en los microservicios: registrar un modelo o enviar una imagen a
     * validar. Se separa de la lectura porque modifica datos que no viven en el
     * telefono y que el resto del equipo ve.
     */
    SERVICES_WRITE("servicios:escribir"),
    ;

    companion object {
        fun fromClaim(value: String): AuthorizationScope? =
            entries.firstOrNull { it.claimValue == value }
    }
}
