package com.cacaoscan.security

import com.cacaoscan.security.authz.AuthorizationScope
import com.cacaoscan.security.authz.LocalTokenService
import com.cacaoscan.security.authz.TokenVerification

/**
 * Sesion autorizada en curso.
 *
 * El token se guarda UNICAMENTE en memoria: no se escribe en preferencias ni en
 * la base de datos. Al cerrar la aplicacion la sesion desaparece, que es el
 * comportamiento deseado para una autorizacion de vida corta y evita el riesgo
 * de dejar un token valido en disco.
 *
 * Une las dos mitades del requisito del profesor:
 *  - AUTENTICACION: la realiza el sistema operativo (AppLock).
 *  - AUTORIZACION: la representa el token emitido aqui, con sus scopes.
 */
class SecuritySession(private val tokenService: LocalTokenService) {

    @Volatile
    private var token: String? = null

    /** Abre una sesion tras una autenticacion correcta. */
    fun open(scopes: Set<AuthorizationScope> = AuthorizationScope.entries.toSet()) {
        token = tokenService.issue(scopes)
    }

    fun close() {
        token = null
    }

    /**
     * Comprueba si la sesion actual permite una accion.
     *
     * Devuelve el detalle de la verificacion y no un simple booleano para que la
     * interfaz pueda distinguir entre "no ha iniciado sesion", "la sesion
     * expiro" y "no tiene ese permiso", que son situaciones distintas para el
     * usuario.
     */
    fun authorize(scope: AuthorizationScope): TokenVerification {
        val current = token ?: return TokenVerification.Malformed
        val verification = tokenService.verify(current, scope)
        if (verification is TokenVerification.Expired) token = null
        return verification
    }

    val isOpen: Boolean
        get() = token?.let {
            tokenService.verify(it) is TokenVerification.Valid
        } ?: false

    /**
     * Token actual, solo para la pantalla de demostracion academica que muestra
     * su estructura. No se expone a ninguna otra parte de la aplicacion.
     */
    fun currentTokenForDemo(): String? = token
}
