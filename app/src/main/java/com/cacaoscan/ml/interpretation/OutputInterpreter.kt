package com.cacaoscan.ml.interpretation

import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.model.Confidence
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.ml.ModelSpec
import com.cacaoscan.ml.OutputFormat

/**
 * Traduce la salida numerica del modelo a una categoria del dominio.
 *
 * Se mantiene separada del interprete por dos razones: es la parte con mas
 * riesgo de error logico (orden de etiquetas, sigmoide invertida) y es la unica
 * que puede probarse por completo sin dispositivo ni modelo (ver
 * OutputInterpreterTest).
 */
class OutputInterpreter(private val spec: ModelSpec) {

    /**
     * @param rawOutput valores crudos de la capa de salida.
     * @param inferenceTimeMs tiempo medido de inferencia.
     * @param origin procedencia del resultado.
     * @param modelVersion identificador del modelo, para trazabilidad.
     */
    fun interpret(
        rawOutput: FloatArray,
        inferenceTimeMs: Long,
        origin: ClassificationOrigin,
        modelVersion: String?,
    ): ClassificationOutput {
        val probabilities = toProbabilities(rawOutput)
        val best = probabilities.maxByOrNull { it.value.fraction }
            ?: error("El mapa de probabilidades nunca puede estar vacio.")

        return ClassificationOutput(
            condition = best.key,
            confidence = best.value,
            probabilities = probabilities,
            inferenceTimeMs = inferenceTimeMs,
            origin = origin,
            modelVersion = modelVersion,
        )
    }

    /**
     * Convierte la salida cruda en una probabilidad por categoria.
     *
     * En SOFTMAX_TWO_CLASS se renormaliza defensivamente: algunos modelos
     * exportados entregan logits en lugar de probabilidades y la suma no da 1.
     */
    fun toProbabilities(rawOutput: FloatArray): Map<PodCondition, Confidence> =
        when (spec.outputFormat) {
            OutputFormat.SIGMOID_SINGLE -> {
                require(rawOutput.isNotEmpty()) {
                    "La salida sigmoide requiere al menos un valor."
                }
                val second = rawOutput[0].coerceIn(0f, 1f)
                mapOf(
                    spec.labelOrder[0] to Confidence.ofFraction(1f - second),
                    spec.labelOrder[1] to Confidence.ofFraction(second),
                )
            }

            OutputFormat.SOFTMAX_TWO_CLASS -> {
                require(rawOutput.size >= spec.labelOrder.size) {
                    "La salida tiene ${rawOutput.size} valores y se esperaban " +
                        "${spec.labelOrder.size}."
                }
                val values = FloatArray(spec.labelOrder.size) { rawOutput[it] }
                val normalized = normalize(values)
                spec.labelOrder.mapIndexed { index, condition ->
                    condition to Confidence.ofFraction(normalized[index])
                }.toMap()
            }
        }

    /**
     * Normaliza a suma 1. Si los valores no son probabilidades (hay negativos o
     * la suma se aleja de 1) se aplica softmax; si ya suman aproximadamente 1 se
     * respetan tal cual para no alterar la salida del modelo.
     */
    private fun normalize(values: FloatArray): FloatArray {
        val sum = values.sum()
        val looksLikeProbabilities = values.none { it < 0f } &&
            kotlin.math.abs(sum - 1f) <= PROBABILITY_TOLERANCE
        if (looksLikeProbabilities) return values

        val max = values.max()
        val exps = FloatArray(values.size) { kotlin.math.exp((values[it] - max).toDouble()).toFloat() }
        val expSum = exps.sum()
        return FloatArray(exps.size) { exps[it] / expSum }
    }

    private companion object {
        const val PROBABILITY_TOLERANCE = 0.02f
    }
}
