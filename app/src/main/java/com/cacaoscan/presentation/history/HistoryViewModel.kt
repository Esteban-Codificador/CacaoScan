package com.cacaoscan.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.domain.model.ScanRecord
import com.cacaoscan.domain.usecase.DeleteAllScansUseCase
import com.cacaoscan.domain.usecase.GetScanHistoryUseCase
import com.cacaoscan.domain.usecase.ResolveScanImageUseCase
import com.cacaoscan.presentation.common.UiState
import com.cacaoscan.security.SecuritySession
import com.cacaoscan.security.authz.AuthorizationScope
import com.cacaoscan.security.authz.TokenVerification
import com.cacaoscan.security.lock.AppLock
import com.cacaoscan.security.lock.AuthenticationResult
import com.cacaoscan.security.lock.LockAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fila del historial ya preparada para pintarse.
 *
 * El ViewModel entrega textos formateados en lugar de objetos de dominio: asi la
 * pantalla no tiene que formatear fechas ni decidir etiquetas, y ese formateo
 * puede probarse sin interfaz.
 */
data class ScanListItem(
    val id: Long,
    val dateText: String,
    val conditionLabel: String,
    val isHealthy: Boolean,
    val confidencePercent: Int,
    val imagePath: String?,
    val isSimulated: Boolean,
)

/**
 * Historial de revisiones (RF06, RNF05).
 *
 * Observa la base de datos mediante Flow, de modo que al guardar una revision
 * nueva la lista se actualiza sola, sin recargas manuales.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    getScanHistory: GetScanHistoryUseCase,
    private val resolveScanImage: ResolveScanImageUseCase,
    private val deleteAllScans: DeleteAllScansUseCase,
    private val appLock: AppLock,
    private val securitySession: SecuritySession,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<UiState<List<ScanListItem>>> = getScanHistory()
        .map { records ->
            if (records.isEmpty()) {
                UiState.Empty("Todavia no ha revisado ninguna vaina.")
            } else {
                UiState.Success(records.map(::toListItem))
            }
        }
        .catch {
            emit(UiState.Error("No se pudo leer el historial guardado en el dispositivo."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading("Abriendo el historial..."),
        )

    /**
     * Borrado completo del historial (accion destructiva e irreversible).
     *
     * Es la unica accion de la aplicacion protegida por el modulo de seguridad:
     *   1. AUTENTICACION: la verifica el sistema operativo (huella o credencial).
     *   2. AUTORIZACION: se emite un token local y se exige el permiso
     *      'history:purge' antes de tocar la base de datos.
     *
     * Si el dispositivo no tiene bloqueo configurado, la accion se deniega y se
     * explica por que, en lugar de dejarla pasar sin comprobacion.
     *
     * El ViewModel no conoce la Activity: el dialogo del sistema se resuelve
     * dentro de la implementacion de [AppLock].
     */
    fun purgeHistory() {
        if (appLock.availability() != LockAvailability.AVAILABLE) {
            _message.value = "Para borrar todo el historial, configure primero el bloqueo de " +
                "pantalla o la huella en los ajustes del telefono."
            return
        }

        viewModelScope.launch {
            val authentication =
                appLock.authenticate("Confirme su identidad para borrar todo el historial")
            if (authentication !is AuthenticationResult.Succeeded) {
                _message.value = "No se borro nada: la identidad no fue confirmada."
                return@launch
            }

            securitySession.open(setOf(AuthorizationScope.HISTORY_PURGE))
            if (securitySession.authorize(AuthorizationScope.HISTORY_PURGE)
                !is TokenVerification.Valid
            ) {
                _message.value = "No se borro nada: la sesion no tiene permiso para esta accion."
                return@launch
            }

            try {
                deleteAllScans()
                _message.value = "Se borro todo el historial."
            } catch (error: Exception) {
                _message.value = "No se pudo borrar el historial."
            } finally {
                // La sesion se cierra en cuanto deja de hacer falta: no se mantiene
                // abierta una autorizacion destructiva mas tiempo del necesario.
                securitySession.close()
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun toListItem(record: ScanRecord): ScanListItem = ScanListItem(
        id = record.id,
        dateText = ScanDateFormatter.format(record.createdAt),
        conditionLabel = record.condition.toLabel(),
        isHealthy = record.condition == PodCondition.HEALTHY,
        confidencePercent = record.confidence.percent,
        imagePath = resolveScanImage(record.imageFileName),
        isSimulated = record.isSimulated,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun PodCondition.toLabel(): String = when (this) {
    PodCondition.HEALTHY -> "Se ve sana"
    PodCondition.MONILIA_AFFECTED -> "Senales de monilia"
}
