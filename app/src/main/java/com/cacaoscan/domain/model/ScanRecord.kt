package com.cacaoscan.domain.model

import java.time.Instant

/**
 * Registro de una revision guardada en el historial local (RF06).
 *
 * @param imageFileName nombre del archivo de imagen en el almacenamiento interno
 *   de la aplicacion. Se guarda el nombre y no una ruta absoluta para que el
 *   registro siga siendo valido si cambia la ruta base de la aplicacion.
 */
data class ScanRecord(
    val id: Long,
    val createdAt: Instant,
    val condition: PodCondition,
    val confidence: Confidence,
    val inferenceTimeMs: Long,
    val origin: ClassificationOrigin,
    val modelVersion: String?,
    val imageFileName: String?,
) {
    val isSimulated: Boolean get() = origin == ClassificationOrigin.SIMULATION

    companion object {
        /** Identificador usado para registros aun no persistidos. */
        const val NOT_PERSISTED: Long = 0L
    }
}
