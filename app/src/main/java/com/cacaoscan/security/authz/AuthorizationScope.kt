package com.cacaoscan.security.authz

/**
 * Permisos que puede otorgar un token local.
 *
 * Deliberadamente NO existe un scope para escanear o clasificar: el flujo
 * principal de CacaoScan (RF01 a RF05) nunca depende de este modulo. Solo se
 * protegen acciones secundarias y destructivas.
 */
enum class AuthorizationScope(val claimValue: String) {
    /** Consultar el historial. */
    HISTORY_READ("history:read"),

    /** Borrar una revision concreta. */
    HISTORY_DELETE("history:delete"),

    /** Borrar todo el historial de una vez. Accion irreversible. */
    HISTORY_PURGE("history:purge"),
    ;

    companion object {
        fun fromClaim(value: String): AuthorizationScope? =
            entries.firstOrNull { it.claimValue == value }
    }
}
