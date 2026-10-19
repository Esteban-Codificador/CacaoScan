package com.cacaoscan.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.domain.usecase.DeleteScanUseCase
import com.cacaoscan.domain.usecase.GetScanByIdUseCase
import com.cacaoscan.domain.usecase.ResolveScanImageUseCase
import com.cacaoscan.navigation.Routes
import com.cacaoscan.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Datos de una revision, ya formateados para la pantalla de detalle. */
data class ScanDetail(
    val id: Long,
    val dateText: String,
    val conditionLabel: String,
    val isHealthy: Boolean,
    val confidencePercent: Int,
    val inferenceTimeMs: Long,
    val modelVersion: String,
    val imagePath: String?,
    val isSimulated: Boolean,
)

/**
 * Detalle de una revision guardada (RF06).
 *
 * El identificador llega por SavedStateHandle desde el argumento de navegacion,
 * de modo que el ViewModel puede recuperarse tras la muerte del proceso sin
 * depender de que la pantalla se lo vuelva a pasar.
 */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getScanById: GetScanByIdUseCase,
    private val resolveScanImage: ResolveScanImageUseCase,
    private val deleteScan: DeleteScanUseCase,
) : ViewModel() {

    private val scanId: Long = savedStateHandle.get<Long>(Routes.ARG_SCAN_ID) ?: -1L

    private val _uiState = MutableStateFlow<UiState<ScanDetail>>(
        UiState.Loading("Abriendo la revision..."),
    )
    val uiState: StateFlow<UiState<ScanDetail>> = _uiState.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        load()
    }

    fun onDelete() {
        if (scanId <= 0L) return
        viewModelScope.launch {
            try {
                deleteScan(scanId)
                _deleted.value = true
            } catch (error: Exception) {
                _uiState.value = UiState.Error("No se pudo eliminar la revision.")
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val record = getScanById(scanId)
                _uiState.value = if (record == null) {
                    UiState.Error("Esta revision ya no esta en el historial.")
                } else {
                    UiState.Success(
                        ScanDetail(
                            id = record.id,
                            dateText = ScanDateFormatter.format(record.createdAt),
                            conditionLabel = record.condition.toLabel(),
                            isHealthy = record.condition == PodCondition.HEALTHY,
                            confidencePercent = record.confidence.percent,
                            inferenceTimeMs = record.inferenceTimeMs,
                            modelVersion = record.modelVersion ?: "sin identificar",
                            imagePath = resolveScanImage(record.imageFileName),
                            isSimulated = record.isSimulated,
                        ),
                    )
                }
            } catch (error: Exception) {
                _uiState.value = UiState.Error("No se pudo leer la revision guardada.")
            }
        }
    }
}
