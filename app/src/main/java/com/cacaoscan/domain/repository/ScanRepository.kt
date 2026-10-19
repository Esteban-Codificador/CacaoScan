package com.cacaoscan.domain.repository

import android.graphics.Bitmap
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.model.ScanRecord
import kotlinx.coroutines.flow.Flow

/**
 * Fuente unica de verdad del historial de revisiones (RF06).
 *
 * Se define como interfaz en la capa de dominio para que los casos de uso y los
 * ViewModels no dependan de Room. Esto permite probarlos con una implementacion
 * en memoria y deja documentado que la persistencia es un detalle sustituible.
 */
interface ScanRepository {

    /**
     * Historial completo, del mas reciente al mas antiguo.
     *
     * Se expone como Flow para que la lista se actualice sola cuando se guarda
     * una revision nueva, sin que la pantalla tenga que recargar nada.
     */
    fun observeHistory(): Flow<List<ScanRecord>>

    /** Devuelve un registro concreto, o null si ya no existe. */
    suspend fun getById(id: Long): ScanRecord?

    /**
     * Guarda una clasificacion junto con su imagen.
     *
     * @param image imagen analizada; se almacena reescalada en el almacenamiento
     *   interno de la aplicacion. Si es null, el registro se guarda sin imagen.
     * @return identificador del registro creado.
     */
    suspend fun save(output: ClassificationOutput, image: Bitmap?): Long

    /** Elimina un registro y su imagen asociada. */
    suspend fun delete(id: Long)

    /** Elimina todo el historial y todas las imagenes almacenadas. */
    suspend fun deleteAll()

    /** Ruta absoluta de la imagen de un registro, o null si no tiene o falta. */
    fun resolveImagePath(fileName: String?): String?
}
