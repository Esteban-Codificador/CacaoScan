package com.cacaoscan.domain.usecase

import android.graphics.Bitmap
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.model.ScanRecord
import com.cacaoscan.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow

/**
 * Guarda una revision en el historial (RF06).
 */
class SaveScanUseCase(private val repository: ScanRepository) {
    suspend operator fun invoke(output: ClassificationOutput, image: Bitmap?): Long =
        repository.save(output, image)
}

/**
 * Observa el historial completo, del mas reciente al mas antiguo (RF06).
 */
class GetScanHistoryUseCase(private val repository: ScanRepository) {
    operator fun invoke(): Flow<List<ScanRecord>> = repository.observeHistory()
}

/**
 * Recupera una revision concreta para la pantalla de detalle (RF06).
 */
class GetScanByIdUseCase(private val repository: ScanRepository) {
    suspend operator fun invoke(id: Long): ScanRecord? = repository.getById(id)
}

/**
 * Traduce el nombre de archivo guardado en el historial a una ruta que la
 * interfaz pueda mostrar. Devuelve null si la imagen ya no existe, para que la
 * pantalla muestre el registro sin foto en lugar de un hueco roto.
 */
class ResolveScanImageUseCase(private val repository: ScanRepository) {
    operator fun invoke(fileName: String?): String? = repository.resolveImagePath(fileName)
}

/**
 * Elimina una revision del historial (RF06).
 */
class DeleteScanUseCase(private val repository: ScanRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

/**
 * Elimina todo el historial. Es una accion destructiva e irreversible: la
 * pantalla debe confirmarla y, ademas, esta protegida por el modulo de
 * autorizacion local (ver com.cacaoscan.security).
 */
class DeleteAllScansUseCase(private val repository: ScanRepository) {
    suspend operator fun invoke() = repository.deleteAll()
}
