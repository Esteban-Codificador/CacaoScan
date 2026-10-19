package com.cacaoscan.data.local.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.max

/**
 * Carga imagenes de la camara o de la galeria como Bitmap listo para analizar.
 *
 * Resuelve tres problemas que, omitidos, degradan la clasificacion en silencio:
 *
 *  1. Memoria: una foto de 12 MP ocupa unos 48 MB descomprimida y agota la
 *     memoria de un dispositivo de gama media (RNF07). Se submuestrea al
 *     decodificar, no despues.
 *  2. Orientacion: muchas camaras guardan la foto girada y anotan la rotacion en
 *     los metadatos EXIF. Sin corregirla, el modelo recibe vainas acostadas.
 *  3. Formato: se fuerza ARGB_8888 porque el preprocesamiento lee canales RGB.
 */
class BitmapLoader(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * @throws IOException si la imagen no se puede leer o no es una imagen valida.
     */
    suspend fun load(uri: Uri): Bitmap = withContext(ioDispatcher) {
        val bounds = readBounds(uri)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("El archivo seleccionado no es una imagen valida.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "No se pudo abrir la imagen." }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("No se pudo decodificar la imagen.")

        applyExifRotation(uri, decoded)
    }

    /** Variante para archivos propios, como la foto recien tomada por la camara. */
    suspend fun load(file: File): Bitmap = load(Uri.fromFile(file))

    private fun readBounds(uri: Uri): BitmapFactory.Options {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "No se pudo abrir la imagen." }
            BitmapFactory.decodeStream(stream, null, options)
        }
        return options
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (max(width, height) / sampleSize > MAX_DECODED_SIDE_PX) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) return@use ExifInterface.ORIENTATION_NORMAL
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private companion object {
        /**
         * Lado mayor tras el submuestreo. Muy por encima de la entrada del
         * modelo (unos 224 px) para no perder detalle antes de tiempo, pero lo
         * bastante contenido para no agotar la memoria en gama media.
         */
        const val MAX_DECODED_SIDE_PX = 1600
    }
}
