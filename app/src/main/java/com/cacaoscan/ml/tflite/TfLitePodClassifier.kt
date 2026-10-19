package com.cacaoscan.ml.tflite

import android.content.Context
import android.graphics.Bitmap
import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.ml.ClassificationException
import com.cacaoscan.ml.ClassifierMetadata
import com.cacaoscan.ml.ModelSpec
import com.cacaoscan.ml.PodClassifier
import com.cacaoscan.ml.interpretation.OutputInterpreter
import com.cacaoscan.ml.preprocessing.BitmapPreprocessor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.system.measureTimeMillis

/**
 * Clasificador real, ejecutado en el dispositivo con TensorFlow Lite.
 *
 * Toda la ejecucion es local: no hay llamadas de red en ningun punto (RF05, RNF02).
 *
 * El modelo se carga con memory mapping en lugar de leerse a memoria: el archivo
 * se mapea desde el APK y el sistema pagina solo lo que hace falta, lo que
 * reduce el consumo de RAM en dispositivos de gama media (RNF07).
 *
 * El interprete se crea una sola vez y se reutiliza. Crearlo en cada
 * clasificacion consumiria buena parte del presupuesto de cinco segundos de
 * RNF01. Como el interprete no es seguro para uso concurrente, las llamadas se
 * serializan con un mutex.
 */
class TfLitePodClassifier(
    context: Context,
    private val spec: ModelSpec,
) : PodClassifier {

    private val preprocessor = BitmapPreprocessor(spec)
    private val outputInterpreter = OutputInterpreter(spec)
    private val mutex = Mutex()

    private val interpreter: Interpreter = try {
        Interpreter(loadModel(context), Interpreter.Options().apply { numThreads = THREADS })
    } catch (error: Exception) {
        throw ClassificationException(
            "No se pudo inicializar el modelo '${spec.assetFileName}'.",
            error,
        )
    }

    override val metadata: ClassifierMetadata = ClassifierMetadata(
        displayName = "Modelo TensorFlow Lite en el dispositivo",
        modelVersion = spec.modelVersion,
        isSimulated = false,
        spec = spec,
    )

    override suspend fun classify(bitmap: Bitmap): ClassificationOutput = mutex.withLock {
        val rawOutput: FloatArray
        val elapsed = measureTimeMillis {
            val input = preprocessor.toInputBuffer(bitmap)
            rawOutput = runInference(input)
        }

        outputInterpreter.interpret(
            rawOutput = rawOutput,
            inferenceTimeMs = elapsed,
            origin = ClassificationOrigin.TRAINED_MODEL,
            modelVersion = spec.modelVersion,
        )
    }

    override fun close() {
        runCatching { interpreter.close() }
    }

    /**
     * Ejecuta el modelo y devuelve la salida como flotantes.
     *
     * El tipo de la capa de salida se consulta al propio modelo en lugar de
     * asumirse: un modelo cuantizado entrega enteros de 8 bits que hay que
     * descuantizar con la escala y el punto cero que el archivo declara.
     */
    private fun runInference(input: ByteBuffer): FloatArray {
        val outputTensor = interpreter.getOutputTensor(0)
        val elementCount = outputTensor.numElements()

        return when (outputTensor.dataType()) {
            DataType.FLOAT32 -> {
                val output = ByteBuffer
                    .allocateDirect(elementCount * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                interpreter.run(input, output)
                output.rewind()
                FloatArray(elementCount) { output.float }
            }

            DataType.UINT8 -> {
                val output = ByteBuffer.allocateDirect(elementCount).order(ByteOrder.nativeOrder())
                interpreter.run(input, output)
                output.rewind()
                val quantization = outputTensor.quantizationParams()
                FloatArray(elementCount) {
                    val raw = output.get().toInt() and 0xFF
                    (raw - quantization.zeroPoint) * quantization.scale
                }
            }

            else -> throw ClassificationException(
                "Tipo de salida no soportado: ${outputTensor.dataType()}. " +
                    "Exporte el modelo con salida FLOAT32 o UINT8.",
            )
        }
    }

    private fun loadModel(context: Context): MappedByteBuffer =
        context.assets.openFd(spec.assetFileName).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }

    private companion object {
        /**
         * Dos hilos es un punto medio razonable en gama media: mas hilos apenas
         * mejoran un modelo pequeno y compiten con la interfaz por la CPU.
         * Conviene ajustarlo midiendo con el modelo real (ver RNF01).
         */
        const val THREADS = 2
    }
}
