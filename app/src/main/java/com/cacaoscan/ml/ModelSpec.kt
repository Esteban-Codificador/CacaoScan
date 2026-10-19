package com.cacaoscan.ml

import com.cacaoscan.domain.model.PodCondition

/**
 * Forma en que el modelo espera recibir los pixeles.
 *
 * Debe coincidir exactamente con el preprocesamiento usado durante el
 * entrenamiento. Un desajuste aqui no produce un error visible: produce
 * predicciones silenciosamente malas, que es el fallo mas dificil de detectar.
 */
enum class InputNormalization {
    /** Float en [0, 1]: pixel / 255. */
    ZERO_TO_ONE,

    /** Float en [-1, 1]: (pixel / 127.5) - 1. Habitual en MobileNet. */
    MINUS_ONE_TO_ONE,

    /** Entero sin signo [0, 255] sin escalar. Habitual en modelos cuantizados. */
    UINT8_RAW,
}

/**
 * Forma de la salida del modelo.
 */
enum class OutputFormat {
    /** Dos valores que ya suman 1 (softmax), en el orden de [ModelSpec.labelOrder]. */
    SOFTMAX_TWO_CLASS,

    /**
     * Un unico valor en [0, 1] (sigmoide) que representa la probabilidad de la
     * SEGUNDA etiqueta de [ModelSpec.labelOrder].
     */
    SIGMOID_SINGLE,
}

/**
 * Contrato del modelo de clasificacion.
 *
 * ATENCION - PARAMETROS PENDIENTES DE CONFIRMAR
 * ---------------------------------------------
 * Los valores por defecto de esta clase son una PROPUESTA razonable, no un
 * hecho: en el momento de escribir este codigo el modelo todavia no ha sido
 * entrenado. Antes de integrar el archivo .tflite real, el equipo de IA debe
 * confirmar y corregir aqui:
 *
 *   1. inputWidth / inputHeight  - resolucion de entrada usada al entrenar.
 *   2. normalization             - escalado exacto aplicado a los pixeles.
 *   3. outputFormat              - softmax de dos salidas o sigmoide de una.
 *   4. labelOrder                - orden de las clases en la capa de salida.
 *   5. modelVersion              - identificador del modelo, para trazabilidad.
 *
 * El procedimiento para obtener estos valores del archivo entrenado esta en
 * /docs/07-integracion-del-modelo.md
 */
data class ModelSpec(
    val assetFileName: String = DEFAULT_ASSET_FILE_NAME,
    val inputWidth: Int = 224,
    val inputHeight: Int = 224,
    val normalization: InputNormalization = InputNormalization.ZERO_TO_ONE,
    val outputFormat: OutputFormat = OutputFormat.SOFTMAX_TWO_CLASS,
    val labelOrder: List<PodCondition> = PodCondition.CANONICAL_ORDER,
    val modelVersion: String = "sin-modelo",
) {
    init {
        require(inputWidth > 0 && inputHeight > 0) {
            "Las dimensiones de entrada deben ser positivas."
        }
        require(labelOrder.size == PodCondition.entries.size) {
            "labelOrder debe contener exactamente las ${PodCondition.entries.size} categorias."
        }
        require(labelOrder.toSet().size == labelOrder.size) {
            "labelOrder no puede repetir categorias."
        }
    }

    companion object {
        /**
         * Nombre esperado del modelo dentro de app/src/main/assets.
         * Si el archivo no existe, la aplicacion arranca en modo demostracion.
         */
        const val DEFAULT_ASSET_FILE_NAME: String = "cacao_pod_classifier.tflite"
    }
}
