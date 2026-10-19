package com.cacaoscan.data.local.file

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Almacenamiento de las imagenes analizadas (RNF06: solo en el dispositivo).
 *
 * Decisiones de seguridad y privacidad:
 *
 *  - Las imagenes van al almacenamiento INTERNO de la aplicacion (filesDir), no
 *    a MediaStore ni a almacenamiento externo. Asi quedan aisladas por el
 *    sandbox de Android: ninguna otra aplicacion puede leerlas y desaparecen al
 *    desinstalar CacaoScan.
 *  - No se expone ningun FileProvider: no hay forma de compartir estos archivos
 *    hacia fuera, porque compartir no forma parte del alcance.
 *  - Los nombres se generan con UUID y nunca provienen de datos externos, lo que
 *    elimina de raiz cualquier intento de escritura fuera del directorio.
 *
 * Sobre el tamano (RNF08): la imagen se reescala antes de guardarse. Guardar la
 * fotografia original, de varios megabytes, haria crecer el almacenamiento sin
 * limite tras unas pocas decenas de revisiones.
 */
class InternalImageStorage(private val context: Context) : ImageStore {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Guarda [bitmap] reescalado como JPEG y devuelve el nombre del archivo.
     *
     * @throws IOException si no se puede escribir en el almacenamiento interno.
     */
    override fun save(bitmap: Bitmap): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(directory, fileName)
        val downscaled = downscale(bitmap)
        try {
            file.outputStream().use { output ->
                val compressed = downscaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                if (!compressed) throw IOException("No se pudo comprimir la imagen.")
            }
        } catch (error: IOException) {
            file.delete()
            throw error
        } finally {
            if (downscaled !== bitmap) downscaled.recycle()
        }
        return fileName
    }

    /** Ruta absoluta de una imagen guardada, o null si el archivo ya no existe. */
    override fun resolvePath(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        if (!SafeFileName.isValid(fileName)) return null
        val file = File(directory, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /** Elimina una imagen. No falla si el archivo ya no existe. */
    override fun delete(fileName: String?) {
        if (fileName.isNullOrBlank() || !SafeFileName.isValid(fileName)) return
        File(directory, fileName).delete()
    }

    /** Elimina todas las imagenes almacenadas. */
    override fun deleteAll() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun downscale(bitmap: Bitmap): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= MAX_STORED_SIDE_PX) return bitmap
        val factor = MAX_STORED_SIDE_PX.toFloat() / longestSide
        val width = (bitmap.width * factor).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * factor).roundToInt().coerceAtLeast(1)
        return bitmap.scale(width, height)
    }

    private companion object {
        const val DIRECTORY_NAME = "scan_images"

        /**
         * Lado mayor de la imagen almacenada. Suficiente para que el productor
         * reconozca la vaina en el historial y muy por debajo del tamano de una
         * fotografia de camara.
         */
        const val MAX_STORED_SIDE_PX = 1024
        const val JPEG_QUALITY = 85
    }
}
