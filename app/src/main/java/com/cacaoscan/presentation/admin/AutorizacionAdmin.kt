package com.cacaoscan.presentation.admin

import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.security.SecuritySession
import com.cacaoscan.security.authz.AuthorizationScope
import com.cacaoscan.security.authz.TokenVerification
import javax.inject.Inject

/**
 * Control de acceso del modulo de administracion.
 *
 * Aqui la autorizacion deja de ser solo una demostracion: cada pantalla que
 * habla con los microservicios exige 'servicios:leer', y cada accion que
 * escribe en ellos exige ademas 'servicios:escribir'. Ambos scopes los emite
 * el login al abrir la sesion, y el token caduca a los 15 minutos.
 */
class AutorizacionAdmin @Inject constructor(
    private val sesion: SecuritySession,
) {

    /** Devuelve null si se permite, o el motivo de la denegacion para mostrarlo. */
    fun comprobar(scope: AuthorizationScope): String? =
        when (val verificacion = sesion.authorize(scope)) {
            is TokenVerification.Valid -> null
            TokenVerification.Expired ->
                "La sesion expiro. Vuelva a iniciar sesion para seguir administrando."
            is TokenVerification.MissingScope ->
                "Su sesion no incluye el permiso '${verificacion.required.claimValue}'."
            TokenVerification.InvalidSignature ->
                "La sesion no es valida. Vuelva a iniciar sesion."
            TokenVerification.Malformed ->
                "No hay ninguna sesion abierta. Inicie sesion para administrar."
        }

    fun puedeLeer(): String? = comprobar(AuthorizationScope.SERVICES_READ)

    fun puedeEscribir(): String? = comprobar(AuthorizationScope.SERVICES_WRITE)
}

/** Texto para el usuario cuando una llamada no fue un exito; null si lo fue. */
fun ResultadoRemoto<*>.mensajeDeFallo(): String? = when (this) {
    is ResultadoRemoto.Exito -> null
    is ResultadoRemoto.Rechazado -> errores.joinToString("\n")
    is ResultadoRemoto.ErrorServicio -> "Error $codigo: $mensaje"
    is ResultadoRemoto.SinConexion -> motivo.mensaje
}
