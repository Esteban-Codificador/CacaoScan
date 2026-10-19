package com.cacaoscan.domain.usecase

import android.graphics.Bitmap
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.ml.ClassificationException
import com.cacaoscan.ml.PodClassifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Clasifica la imagen de una vaina (RF03).
 *
 * Su valor no es envolver una llamada: es garantizar que la inferencia SIEMPRE
 * ocurre fuera del hilo principal y que la imagen se valida antes de llegar al
 * modelo. Si esta regla viviera en el ViewModel habria que repetirla cada vez
 * que aparezca una pantalla nueva que clasifique.
 */
class ClassifyPodImageUseCase(
    private val classifier: PodClassifier,
    private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(bitmap: Bitmap): ClassificationOutput =
        withContext(defaultDispatcher) {
            validate(bitmap)
            classifier.classify(bitmap)
        }

    private fun validate(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            throw ClassificationException("La imagen ya no esta disponible en memoria.")
        }
        if (bitmap.width < MIN_SIDE_PX || bitmap.height < MIN_SIDE_PX) {
            throw ClassificationException(
                "La imagen es demasiado pequena (${bitmap.width}x${bitmap.height}).",
            )
        }
    }

    private companion object {
        /**
         * Por debajo de este tamano la imagen no aporta detalle suficiente y
         * ampliarla solo produciria una prediccion sin fundamento.
         */
        const val MIN_SIDE_PX = 64
    }
}
