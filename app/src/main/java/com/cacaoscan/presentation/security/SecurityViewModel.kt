package com.cacaoscan.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.security.SecuritySession
import com.cacaoscan.security.authz.AuthorizationScope
import com.cacaoscan.security.authz.TokenVerification
import com.cacaoscan.security.lock.AppLock
import com.cacaoscan.security.lock.AuthenticationResult
import com.cacaoscan.security.lock.LockAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Base64
import javax.inject.Inject

/** Token descompuesto para poder mostrarlo en la pantalla de demostracion. */
data class DecodedToken(
    val header: String,
    val payload: String,
    val signaturePreview: String,
)

data class SecurityUiState(
    val availability: LockAvailability = LockAvailability.UNAVAILABLE,
    val sessionOpen: Boolean = false,
    val decodedToken: DecodedToken? = null,
    val lastCheck: String? = null,
    val message: String? = null,
)

/**
 * Pantalla de demostracion academica de autenticacion y autorizacion locales.
 *
 * Existe para poder mostrar y explicar el mecanismo. NINGUNA funcion principal
 * de CacaoScan depende de esta pantalla: si se elimina, la aplicacion sigue
 * escaneando, clasificando y guardando revisiones igual.
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val appLock: AppLock,
    private val securitySession: SecuritySession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            availability = appLock.availability(),
            sessionOpen = securitySession.isOpen,
        ),
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    /**
     * Paso 1: AUTENTICACION. La realiza el sistema operativo.
     * Paso 2: AUTORIZACION. Si la identidad se confirma, se emite el token local.
     */
    fun authenticate() {
        viewModelScope.launch {
            when (appLock.authenticate("Abrir una sesion autorizada local")) {
                AuthenticationResult.Succeeded -> {
                    securitySession.open()
                    _uiState.update {
                        it.copy(
                            sessionOpen = true,
                            decodedToken = decodeCurrentToken(),
                            message = "Sesion abierta. El token es valido durante 15 minutos.",
                        )
                    }
                }

                AuthenticationResult.Cancelled -> _uiState.update {
                    it.copy(message = "Autenticacion cancelada.")
                }

                is AuthenticationResult.Failed -> _uiState.update {
                    it.copy(message = "No se pudo verificar su identidad.")
                }
            }
        }
    }

    fun closeSession() {
        securitySession.close()
        _uiState.update {
            it.copy(
                sessionOpen = false,
                decodedToken = null,
                lastCheck = null,
                message = "Sesion cerrada. El token se descarto de la memoria.",
            )
        }
    }

    /** Comprueba un permiso concreto contra el token actual. */
    fun checkScope(scope: AuthorizationScope) {
        val result = when (val verification = securitySession.authorize(scope)) {
            is TokenVerification.Valid ->
                "PERMITIDO - el token incluye '${scope.claimValue}'."

            is TokenVerification.MissingScope ->
                "DENEGADO - el token no incluye '${verification.required.claimValue}'."

            TokenVerification.Expired ->
                "DENEGADO - el token expiro. Vuelva a autenticarse."

            TokenVerification.InvalidSignature ->
                "DENEGADO - la firma no es valida."

            TokenVerification.Malformed ->
                "DENEGADO - no hay ninguna sesion abierta."
        }
        _uiState.update { it.copy(lastCheck = result, sessionOpen = securitySession.isOpen) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun decodeCurrentToken(): DecodedToken? {
        val token = securitySession.currentTokenForDemo() ?: return null
        val parts = token.split('.')
        if (parts.size != 3) return null
        val decoder = Base64.getUrlDecoder()
        return DecodedToken(
            header = String(decoder.decode(parts[0])),
            payload = String(decoder.decode(parts[1])),
            signaturePreview = parts[2].take(24) + "...",
        )
    }
}
