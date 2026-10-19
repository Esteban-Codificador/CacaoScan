package com.cacaoscan.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.di.IoDispatcher
import com.cacaoscan.security.SecuritySession
import com.cacaoscan.security.credentials.AdminCredentialStore
import com.cacaoscan.security.credentials.LoginResult
import com.cacaoscan.security.credentials.PasswordHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface LoginUiState {
    /** Comprobando si ya existe una contrasena creada. */
    data object Loading : LoginUiState

    /** Primer arranque: hay que crear la contrasena. */
    data class Setup(val error: String? = null, val busy: Boolean = false) : LoginUiState

    /** Arranques posteriores: hay que introducir la contrasena. */
    data class Authenticate(
        val error: String? = null,
        val lockedSeconds: Long = 0L,
        val busy: Boolean = false,
    ) : LoginUiState {
        val isLocked: Boolean get() = lockedSeconds > 0L
    }

    /** Acceso concedido. */
    data object Authenticated : LoginUiState
}

/**
 * Acceso administrativo al abrir la aplicacion.
 *
 * Flujo:
 *  1. Primer arranque  -> crear contrasena (minimo 8 caracteres, con confirmacion).
 *  2. Arranques siguientes -> introducir contrasena.
 *  3. Tras 5 fallos seguidos -> bloqueo temporal persistido.
 *
 * La derivacion PBKDF2 es intencionadamente costosa, asi que se ejecuta siempre
 * fuera del hilo principal para no congelar la interfaz.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val credentials: AdminCredentialStore,
    private val securitySession: SecuritySession,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val configured = withContext(ioDispatcher) { credentials.isConfigured() }
            _uiState.value = if (configured) {
                LoginUiState.Authenticate(lockedSeconds = pendingLock())
            } else {
                LoginUiState.Setup()
            }
            if (configured) startLockCountdown()
        }
    }

    fun onCreatePassword(password: String, confirmation: String) {
        if (password.length < PasswordHasher.MIN_LENGTH) {
            _uiState.value = LoginUiState.Setup(
                error = "La contrasena debe tener al menos ${PasswordHasher.MIN_LENGTH} caracteres.",
            )
            return
        }
        if (password != confirmation) {
            _uiState.value = LoginUiState.Setup(error = "Las dos contrasenas no coinciden.")
            return
        }

        _uiState.value = LoginUiState.Setup(busy = true)
        viewModelScope.launch {
            val characters = password.toCharArray()
            try {
                withContext(ioDispatcher) { credentials.createPassword(characters) }
                grantAccess()
            } catch (error: Exception) {
                _uiState.value = LoginUiState.Setup(error = "No se pudo guardar la contrasena.")
            } finally {
                characters.fill('\u0000')
            }
        }
    }

    fun onSubmitPassword(password: String) {
        val current = _uiState.value
        if (current !is LoginUiState.Authenticate || current.busy || current.isLocked) return

        _uiState.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            val characters = password.toCharArray()
            try {
                when (val result = withContext(ioDispatcher) { credentials.verify(characters) }) {
                    LoginResult.Success -> grantAccess()

                    is LoginResult.Wrong -> _uiState.value = LoginUiState.Authenticate(
                        error = "Contrasena incorrecta. " +
                            "Le quedan ${result.remainingAttempts} intentos.",
                    )

                    is LoginResult.Locked -> {
                        _uiState.value = LoginUiState.Authenticate(
                            error = "Demasiados intentos fallidos.",
                            lockedSeconds = result.secondsRemaining,
                        )
                        startLockCountdown()
                    }

                    LoginResult.NotConfigured -> _uiState.value = LoginUiState.Setup()
                }
            } finally {
                characters.fill('\u0000')
            }
        }
    }

    /**
     * Abre la sesion autorizada. Desde aqui el token local gobierna las acciones
     * sensibles, igual que antes: el login decide QUIEN entra, los scopes
     * deciden QUE puede hacer.
     */
    private fun grantAccess() {
        securitySession.open()
        _uiState.value = LoginUiState.Authenticated
    }

    private suspend fun pendingLock(): Long =
        withContext(ioDispatcher) { credentials.lockedSecondsRemaining() }

    /** Refresca el contador del bloqueo una vez por segundo hasta que expire. */
    private fun startLockCountdown() {
        viewModelScope.launch {
            while (true) {
                val remaining = pendingLock()
                val current = _uiState.value
                if (current !is LoginUiState.Authenticate) return@launch
                if (current.lockedSeconds != remaining) {
                    _uiState.value = current.copy(lockedSeconds = remaining)
                }
                if (remaining <= 0L) return@launch
                delay(1_000)
            }
        }
    }
}
