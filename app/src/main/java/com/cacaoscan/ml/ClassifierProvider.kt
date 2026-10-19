package com.cacaoscan.ml

import android.content.Context
import android.util.Log
import com.cacaoscan.ml.tflite.TfLitePodClassifier
import com.cacaoscan.ml.mock.MockPodClassifier

/**
 * Decide que clasificador usa la aplicacion.
 *
 * Regla unica: si el archivo del modelo entrenado esta presente en assets, se
 * usa el modelo real con TensorFlow Lite; si no esta, se usa el clasificador de
 * demostracion y la interfaz muestra el aviso correspondiente.
 *
 * Integrar el modelo real consiste por tanto en copiar el archivo .tflite en
 * app/src/main/assets y confirmar los parametros de [ModelSpec]. No hay que
 * tocar ViewModels ni pantallas (ver /docs/07-integracion-del-modelo.md).
 *
 * Si el modelo existe pero falla al cargarse (archivo corrupto, formato no
 * soportado), se vuelve al modo demostracion en lugar de dejar la aplicacion
 * inutilizable: el usuario vera claramente que los resultados son simulados.
 */
class ClassifierProvider(
    private val context: Context,
    private val spec: ModelSpec = ModelSpec(),
) {

    fun create(): PodClassifier {
        if (!isModelAssetPresent()) {
            Log.i(
                TAG,
                "No se encontro '${spec.assetFileName}' en assets. " +
                    "La aplicacion arranca en MODO DEMOSTRACION: los resultados son simulados.",
            )
            return MockPodClassifier(spec)
        }

        return try {
            TfLitePodClassifier(context, spec).also {
                Log.i(TAG, "Modelo '${spec.assetFileName}' cargado. Version: ${spec.modelVersion}.")
            }
        } catch (error: ClassificationException) {
            Log.e(TAG, "El modelo existe pero no pudo cargarse. Se usa el modo demostracion.", error)
            MockPodClassifier(spec)
        }
    }

    private fun isModelAssetPresent(): Boolean = runCatching {
        context.assets.open(spec.assetFileName).use { true }
    }.getOrDefault(false)

    private companion object {
        const val TAG = "ClassifierProvider"
    }
}
