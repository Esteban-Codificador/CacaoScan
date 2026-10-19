package com.cacaoscan.ml.interpretation

import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.ml.ModelSpec
import com.cacaoscan.ml.OutputFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La interpretacion de la salida es la parte con mas riesgo de error silencioso
 * del flujo de IA: un orden de etiquetas invertido no produce ningun fallo
 * visible, solo diagnosticos equivocados. Por eso se prueba de forma exhaustiva.
 */
class OutputInterpreterTest {

    private val softmaxSpec = ModelSpec(outputFormat = OutputFormat.SOFTMAX_TWO_CLASS)
    private val sigmoidSpec = ModelSpec(outputFormat = OutputFormat.SIGMOID_SINGLE)

    @Test
    fun `softmax elige la clase de mayor probabilidad`() {
        val result = OutputInterpreter(softmaxSpec).interpret(
            rawOutput = floatArrayOf(0.82f, 0.18f),
            inferenceTimeMs = 120,
            origin = ClassificationOrigin.TRAINED_MODEL,
            modelVersion = "v1",
        )

        assertEquals(PodCondition.HEALTHY, result.condition)
        assertEquals(82, result.confidence.percent)
        assertEquals(18, result.probabilities.getValue(PodCondition.MONILIA_AFFECTED).percent)
    }

    @Test
    fun `softmax respeta el orden de etiquetas declarado en el ModelSpec`() {
        val invertedSpec = softmaxSpec.copy(
            labelOrder = listOf(PodCondition.MONILIA_AFFECTED, PodCondition.HEALTHY),
        )

        val result = OutputInterpreter(invertedSpec).interpret(
            rawOutput = floatArrayOf(0.9f, 0.1f),
            inferenceTimeMs = 0,
            origin = ClassificationOrigin.TRAINED_MODEL,
            modelVersion = "v1",
        )

        assertEquals(PodCondition.MONILIA_AFFECTED, result.condition)
    }

    @Test
    fun `aplica softmax cuando el modelo entrega logits en lugar de probabilidades`() {
        val probabilities = OutputInterpreter(softmaxSpec).toProbabilities(floatArrayOf(2.0f, -1.0f))

        val healthy = probabilities.getValue(PodCondition.HEALTHY).fraction
        val affected = probabilities.getValue(PodCondition.MONILIA_AFFECTED).fraction

        assertEquals(1.0f, healthy + affected, 0.001f)
        assertTrue("La clase con mayor logit debe tener mayor probabilidad", healthy > affected)
    }

    @Test
    fun `no altera una salida que ya suma uno`() {
        val probabilities = OutputInterpreter(softmaxSpec).toProbabilities(floatArrayOf(0.3f, 0.7f))

        assertEquals(0.3f, probabilities.getValue(PodCondition.HEALTHY).fraction, 0.0001f)
        assertEquals(0.7f, probabilities.getValue(PodCondition.MONILIA_AFFECTED).fraction, 0.0001f)
    }

    @Test
    fun `la sigmoide representa la probabilidad de la segunda etiqueta`() {
        val probabilities = OutputInterpreter(sigmoidSpec).toProbabilities(floatArrayOf(0.75f))

        assertEquals(0.25f, probabilities.getValue(PodCondition.HEALTHY).fraction, 0.0001f)
        assertEquals(0.75f, probabilities.getValue(PodCondition.MONILIA_AFFECTED).fraction, 0.0001f)
    }

    @Test
    fun `la sigmoide recorta valores fuera de rango`() {
        val probabilities = OutputInterpreter(sigmoidSpec).toProbabilities(floatArrayOf(1.4f))

        assertEquals(1f, probabilities.getValue(PodCondition.MONILIA_AFFECTED).fraction, 0.0001f)
        assertEquals(0f, probabilities.getValue(PodCondition.HEALTHY).fraction, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza una salida con menos valores de los esperados`() {
        OutputInterpreter(softmaxSpec).toProbabilities(floatArrayOf(0.5f))
    }

    @Test
    fun `conserva el tiempo de inferencia y el origen del resultado`() {
        val result = OutputInterpreter(softmaxSpec).interpret(
            rawOutput = floatArrayOf(0.6f, 0.4f),
            inferenceTimeMs = 1234,
            origin = ClassificationOrigin.SIMULATION,
            modelVersion = "demo",
        )

        assertEquals(1234L, result.inferenceTimeMs)
        assertTrue(result.isSimulated)
        assertEquals("demo", result.modelVersion)
    }
}
