package com.cacaoscan.ml.preprocessing

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.cacaoscan.ml.InputNormalization
import com.cacaoscan.ml.ModelSpec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Convierte un Bitmap en el tensor de entrada que espera el modelo.
 *
 * Esta clase no depende de LiteRT ni de TensorFlow: produce un ByteBuffer, que
 * es lo unico que el interprete necesita. Gracias a eso es comprobable con
 * pruebas unitarias corrientes y no cambia cuando se cambia el runtime de
 * inferencia.
 *
 * El recorte central se aplica ANTES de reescalar: deformar la vaina estirando
 * la imagen a un cuadrado alteraria su forma, que es justamente una de las
 * caracteristicas que el modelo aprende.
 */
class BitmapPreprocessor(private val spec: ModelSpec) {

    /** Numero de bytes que ocupara el tensor de entrada. */
    val inputByteCount: Int
        get() {
            val pixels = spec.inputWidth * spec.inputHeight * CHANNELS
            return when (spec.normalization) {
                InputNormalization.UINT8_RAW -> pixels
                else -> pixels * Float.SIZE_BYTES
            }
        }

    /**
     * Recorta el centro de [bitmap], lo reescala al tamano de entrada y lo
     * normaliza segun [ModelSpec.normalization].
     */
    fun toInputBuffer(bitmap: Bitmap): ByteBuffer {
        val prepared = resize(cropCenterSquare(bitmap))
        val buffer = ByteBuffer.allocateDirect(inputByteCount).order(ByteOrder.nativeOrder())

        val pixels = IntArray(prepared.width * prepared.height)
        prepared.getPixels(pixels, 0, prepared.width, 0, 0, prepared.width, prepared.height)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            when (spec.normalization) {
                InputNormalization.UINT8_RAW -> {
                    buffer.put(r.toByte())
                    buffer.put(g.toByte())
                    buffer.put(b.toByte())
                }

                InputNormalization.ZERO_TO_ONE -> {
                    buffer.putFloat(r / 255f)
                    buffer.putFloat(g / 255f)
                    buffer.putFloat(b / 255f)
                }

                InputNormalization.MINUS_ONE_TO_ONE -> {
                    buffer.putFloat(r / 127.5f - 1f)
                    buffer.putFloat(g / 127.5f - 1f)
                    buffer.putFloat(b / 127.5f - 1f)
                }
            }
        }

        if (prepared !== bitmap) prepared.recycle()
        buffer.rewind()
        return buffer
    }

    /** Recorta el cuadrado central, conservando la proporcion original. */
    fun cropCenterSquare(bitmap: Bitmap): Bitmap {
        val side = min(bitmap.width, bitmap.height)
        if (side == bitmap.width && side == bitmap.height) return bitmap
        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        return Bitmap.createBitmap(bitmap, left, top, side, side)
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        if (bitmap.width == spec.inputWidth && bitmap.height == spec.inputHeight) return bitmap
        val scaled = bitmap.scale(spec.inputWidth, spec.inputHeight)
        return scaled
    }

    private companion object {
        /** RGB. El canal alfa se descarta: las fotografias de campo son opacas. */
        const val CHANNELS = 3
    }
}
