package com.cacaoscan.presentation.scan

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.data.local.file.BitmapLoader
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.usecase.ClassifyPodImageUseCase
import com.cacaoscan.domain.usecase.SaveScanUseCase
import com.cacaoscan.ml.ClassificationException
import com.cacaoscan.ml.PodClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Etapa del flujo de escaneo. */
enum class ScanStep {
    /** Esperando a que el usuario tome o elija una foto. */
    CAPTURE,

    /** Hay una imagen y el usuario decide si analizarla o repetirla. */
    PREVIEW,

    /** Inferencia en curso. */
    CLASSIFYING,

    /** Hay resultado disponible. */
    RESULT,
}

data class ScanUiState(
    val step: ScanStep = ScanStep.CAPTURE,
    val image: Bitmap? = null,
    val result: ClassificationOutput? = null,
    val isSaving: Boolean = false,
    val savedRecordId: Long? = null,
    val errorMessage: String? = null,
    /** true cuando el clasificador activo es el de demostracion. */
    val isSimulatedClassifier: Boolean = true,
) {
    val canAnalyze: Boolean get() = image != null && step == ScanStep.PREVIEW
    val isSaved: Boolean get() = savedRecordId != null
}

/**
 * Estado del flujo de escaneo (RF01 a RF04).
 *
 * Vive en el subgrafo de navegacion, de modo que la pantalla de captura y la de
 * resultado comparten instancia. Sobrevive a los cambios de configuracion, que
 * es lo que evita perder una clasificacion en curso si el telefono gira.
 *
 * No contiene logica de inferencia ni de persistencia: delega en los casos de
 * uso. Su unica responsabilidad es decidir que ve la pantalla en cada momento.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val classifyPodImage: ClassifyPodImageUseCase,
    private val saveScan: SaveScanUseCase,
    private val bitmapLoader: BitmapLoader,
    classifier: PodClassifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScanUiState(isSimulatedClassifier = classifier.metadata.isSimulated),
    )
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /** Imagen elegida desde la galeria (RF02). */
    fun onImagePicked(uri: Uri) {
        loadImage { bitmapLoader.load(uri) }
    }

    /** Fotografia tomada con la camara (RF01). */
    fun onPhotoCaptured(file: File) {
        loadImage { bitmapLoader.load(file) }
    }

    /** El usuario descarta la imagen y vuelve a capturar. */
    fun onRetake() {
        val previous = _uiState.value.image
        _uiState.update {
            it.copy(
                step = ScanStep.CAPTURE,
                image = null,
                result = null,
                savedRecordId = null,
                errorMessage = null,
            )
        }
        previous?.recycle()
    }

    /** Lanza la clasificacion de la imagen actual (RF03). */
    fun onAnalyze() {
        val bitmap = _uiState.value.image ?: return
        if (_uiState.value.step == ScanStep.CLASSIFYING) return

        _uiState.update { it.copy(step = ScanStep.CLASSIFYING, errorMessage = null) }
        viewModelScope.launch {
            try {
                val output = classifyPodImage(bitmap)
                _uiState.update { it.copy(step = ScanStep.RESULT, result = output) }
            } catch (error: ClassificationException) {
                _uiState.update {
                    it.copy(
                        step = ScanStep.PREVIEW,
                        errorMessage = "No se pudo analizar la imagen. Intente con otra fotografia.",
                    )
                }
            }
        }
    }

    /** Guarda el resultado en el historial local (RF06). */
    fun onSave() {
        val state = _uiState.value
        val output = state.result ?: return
        if (state.isSaving || state.isSaved) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val id = saveScan(output, state.image)
                _uiState.update { it.copy(isSaving = false, savedRecordId = id) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo guardar la revision en el historial.",
                    )
                }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadImage(source: suspend () -> Bitmap) {
        viewModelScope.launch {
            val previous = _uiState.value.image
            try {
                val bitmap = source()
                _uiState.update {
                    it.copy(
                        step = ScanStep.PREVIEW,
                        image = bitmap,
                        result = null,
                        savedRecordId = null,
                        errorMessage = null,
                    )
                }
                previous?.recycle()
            } catch (error: IOException) {
                _uiState.update {
                    it.copy(errorMessage = "No se pudo abrir la imagen seleccionada.")
                }
            }
        }
    }

    override fun onCleared() {
        _uiState.value.image?.recycle()
        super.onCleared()
    }
}
