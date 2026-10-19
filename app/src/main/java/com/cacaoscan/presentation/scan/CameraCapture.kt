package com.cacaoscan.presentation.scan

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * Controlador de la camara expuesto a la pantalla.
 *
 * La pantalla solo necesita poder disparar la foto; todo lo relativo a CameraX
 * queda encapsulado aqui.
 */
class CameraController internal constructor(
    private val context: Context,
    private val imageCapture: ImageCapture,
) {
    fun takePhoto(onCaptured: (File) -> Unit, onError: (String) -> Unit) {
        val file = File(context.cacheDir, "captura_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onCaptured(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    onError("No se pudo tomar la fotografia. Intente de nuevo.")
                }
            },
        )
    }
}

/**
 * Vista previa de la camara (RF01).
 *
 * La foto se escribe en el directorio de cache de la aplicacion, no en la
 * galeria del telefono: CacaoScan no debe llenar el carrete del usuario con
 * imagenes de trabajo, y el archivo se descarta en cuanto se decodifica.
 */
@Composable
fun CameraPreview(
    onControllerReady: (CameraController) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val previewView = PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(viewContext)

            providerFuture.addListener(
                {
                    try {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build()
                        preview.surfaceProvider = previewView.surfaceProvider

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                        onControllerReady(CameraController(viewContext, imageCapture))
                    } catch (error: Exception) {
                        onError("No se pudo iniciar la camara del dispositivo.")
                    }
                },
                ContextCompat.getMainExecutor(viewContext),
            )
            previewView
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }
}
