package com.cacaoscan.domain.model

/**
 * Origen del resultado mostrado al usuario.
 *
 * Existe para que sea imposible confundir una simulacion con una clasificacion
 * real. Viaja con el resultado hasta la interfaz y hasta el historial, de modo
 * que un registro simulado queda marcado como tal de forma permanente.
 */
enum class ClassificationOrigin {
    /** Resultado producido por el modelo entrenado ejecutado en el dispositivo. */
    TRAINED_MODEL,

    /**
     * Resultado producido por el clasificador de demostracion, que NO analiza la
     * imagen. Solo existe para poder recorrer el flujo de la aplicacion mientras
     * el equipo entrena el modelo real.
     */
    SIMULATION,
}

/**
 * Resultado de clasificar una imagen.
 *
 * @param condition categoria elegida.
 * @param confidence confianza del modelo en esa categoria.
 * @param probabilities probabilidad por categoria, en orden canonico. Se conserva
 *   completa porque es lo que permite auditar el resultado y evaluar el modelo.
 * @param inferenceTimeMs tiempo de inferencia medido en el dispositivo. Es la
 *   evidencia de cumplimiento de RNF01 (menos de 5 segundos); se mide, no se supone.
 * @param origin ver [ClassificationOrigin].
 * @param modelVersion identificador del modelo que produjo el resultado, o null
 *   si no hay modelo.
 */
data class ClassificationOutput(
    val condition: PodCondition,
    val confidence: Confidence,
    val probabilities: Map<PodCondition, Confidence>,
    val inferenceTimeMs: Long,
    val origin: ClassificationOrigin,
    val modelVersion: String?,
) {
    val isSimulated: Boolean get() = origin == ClassificationOrigin.SIMULATION
}
