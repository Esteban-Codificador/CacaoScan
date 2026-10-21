package com.cacaoscan.data.local.file

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.cacaoscan.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Lo que el microservicio de Control de Calidad necesita saber de una foto. */
data class MetadatosImagen(
    val nombre: String,
    val ancho: Int,
    val alto: Int,
    val pesoKb: Double,
)

/**
 * Lee nombre, dimensiones y peso de una imagen elegida en el telefono.
 *
 * PRIVACIDAD. Solo se leen metadatos: las dimensiones se obtienen con
 * inJustDecodeBounds, que lee la cabecera del archivo sin decodificar los
 * pixeles. Al microservicio viajan esos cuatro datos y las clases que anota
 * quien administra; la fotografia en si nunca sale del telefono.
 */
class LectorMetadatosImagen @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun leer(uri: Uri): MetadatosImagen? = withContext(ioDispatcher) {
        val resolver = context.contentResolver

        var nombre: String? = null
        var bytes: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val iNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val iTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (iNombre >= 0 && !cursor.isNull(iNombre)) nombre = cursor.getString(iNombre)
                    if (iTamano >= 0 && !cursor.isNull(iTamano)) bytes = cursor.getLong(iTamano)
                }
            }

        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return@withContext null

        MetadatosImagen(
            nombre = conExtension(nombre ?: "imagen", resolver.getType(uri)),
            ancho = opciones.outWidth,
            alto = opciones.outHeight,
            pesoKb = (bytes ?: 0L) / 1024.0,
        )
    }

    /**
     * El servicio decide el formato por la extension del nombre. Algunos
     * proveedores devuelven nombres sin extension, y entonces se deduce del tipo
     * MIME: de lo contrario una JPEG valida se rechazaria por "formato desconocido".
     */
    private fun conExtension(nombre: String, tipoMime: String?): String {
        if ('.' in nombre) return nombre
        val extension = tipoMime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return if (extension != null) "$nombre.$extension" else nombre
    }
}
