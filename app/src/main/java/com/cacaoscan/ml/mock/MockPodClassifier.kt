package com.cacaoscan.ml.mock

import android.graphics.Bitmap
import androidx.core.graphics.get
import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.ml.ClassifierMetadata
import com.cacaoscan.ml.ModelSpec
import com.cacaoscan.ml.PodClassifier
import com.cacaoscan.ml.interpretation.OutputInterpreter
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.system.measureTimeMillis

/**
 * CLASIFICADOR DE DEMOSTRACION - NO ES INTELIGENCIA ARTIFICIAL
 * =============================================================
 *
 * Esta clase NO analiza el contenido de la imagen y NO tiene ninguna capacidad
 * de detectar monilia. Existe con un unico proposito: permitir recorrer y probar
 * el flujo completo de la aplicacion (captura, resultado, guardado, historial)
 * mientras el equipo entrena el modelo real.
 *
 * Como produce el resultado: calcula una huella numerica a partir de unos pocos
 * pixeles de la imagen y deriva de ella una categoria. El resultado es, por
 * tanto, DETERMINISTA (la misma foto da siempre lo mismo, lo que hace las
 * pruebas reproducibles) pero ARBITRARIO respecto al estado real de la vaina.
 *
 * Todo resultado producido aqui se marca con [ClassificationOrigin.SIMULATION],
 * lo que obliga a la interfaz a mostrar el aviso de modo demostracion y deja el
 * registro del historial marcado como simulado de forma permanente.
 *
 * Se elimina del proyecto, o deja de seleccionarse, cuando exista el .tflite
 * real (ver ClassifierModule y /docs/07-integracion-del-modelo.md).
 */
class MockPodClassifier(
    private val spec: ModelSpec = ModelSpec(),
) : PodClassifier {

    private val interpreter = OutputInterpreter(spec)

    override val metadata: ClassifierMetadata = ClassifierMetadata(
        displayName = "Clasificador de demostracion",
        modelVersion = SIMULATED_VERSION,
        isSimulated = true,
        spec = spec,
    )

    override suspend fun classify(bitmap: Bitmap): ClassificationOutput {
        val rawOutput: FloatArray
        val elapsed = measureTimeMillis {
            // Retardo artificial: reproduce la latencia de una inferencia real
            // para poder validar los estados de carga de la interfaz.
            delay(SIMULATED_LATENCY_MS)
            rawOutput = simulatedOutputFor(bitmap)
        }

        return interpreter.interpret(
            rawOutput = rawOutput,
            inferenceTimeMs = elapsed,
            origin = ClassificationOrigin.SIMULATION,
            modelVersion = SIMULATED_VERSION,
        )
    }

    override fun close() = Unit

    /**
     * Huella determinista de la imagen. No mide nada relacionado con la sanidad
     * de la vaina: solo garantiza que la misma imagen produzca siempre la misma
     * salida simulada.
     */
    private fun simulatedOutputFor(bitmap: Bitmap): FloatArray {
        var fingerprint = 17
        val stepX = (bitmap.width / SAMPLE_GRID).coerceAtLeast(1)
        val stepY = (bitmap.height / SAMPLE_GRID).coerceAtLeast(1)
        var x = 0
        while (x < bitmap.width) {
            var y = 0
            while (y < bitmap.height) {
                fingerprint = fingerprint * 31 + bitmap[x, y]
                y += stepY
            }
            x += stepX
        }

        val bucket = fingerprint.absoluteValue % 1000
        // Confianza simulada en un rango medio-alto, nunca 100%: presentar una
        // certeza absoluta seria enganoso incluso en una demostracion.
        val firstProbability = 0.55f + (bucket % 400) / 1000f
        return if (bucket % 2 == 0) {
            floatArrayOf(firstProbability, 1f - firstProbability)
        } else {
            floatArrayOf(1f - firstProbability, firstProbability)
        }
    }

    private companion object {
        const val SIMULATED_VERSION = "demo-sin-modelo"
        const val SIMULATED_LATENCY_MS = 700L
        const val SAMPLE_GRID = 16
    }
}
