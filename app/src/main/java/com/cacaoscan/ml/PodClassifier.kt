package com.cacaoscan.ml

import android.graphics.Bitmap
import com.cacaoscan.domain.model.ClassificationOutput

/**
 * Contrato de clasificacion de una vaina de cacao.
 *
 * Es la frontera que separa la inteligencia artificial del resto de la
 * aplicacion. Ni la interfaz ni los ViewModels conocen TensorFlow, LiteRT ni
 * ningun detalle de inferencia: solo conocen este contrato. Gracias a eso, el
 * clasificador de demostracion puede sustituirse por el modelo real sin
 * modificar ninguna pantalla (ver /docs/07-integracion-del-modelo.md).
 *
 * Implementaciones:
 *  - [com.cacaoscan.ml.mock.MockPodClassifier] - demostracion, no analiza la imagen.
 *  - TfLitePodClassifier - modelo real (FASE 11).
 */
interface PodClassifier {

    /** Descripcion del clasificador activo, para mostrar y registrar trazabilidad. */
    val metadata: ClassifierMetadata

    /**
     * Clasifica un bitmap ya corregido de orientacion.
     *
     * La implementacion es responsable de su propio preprocesamiento y de medir
     * el tiempo de inferencia. Debe poder llamarse desde un dispatcher de
     * background; nunca desde el hilo principal.
     *
     * @throws ClassificationException si la inferencia no puede completarse.
     */
    suspend fun classify(bitmap: Bitmap): ClassificationOutput

    /** Libera los recursos nativos del interprete. Idempotente. */
    fun close()
}

/**
 * Metadatos del clasificador activo.
 *
 * @param isSimulated true cuando el resultado NO proviene de un modelo entrenado.
 *   La interfaz usa este dato para mostrar el aviso de modo demostracion.
 */
data class ClassifierMetadata(
    val displayName: String,
    val modelVersion: String,
    val isSimulated: Boolean,
    val spec: ModelSpec,
)

/** Fallo durante la clasificacion. El mensaje tecnico no se muestra al usuario. */
class ClassificationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
