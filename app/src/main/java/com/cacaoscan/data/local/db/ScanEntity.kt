package com.cacaoscan.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila del historial de revisiones.
 *
 * Los enumerados se guardan como texto y no como ordinal: un ordinal se rompe en
 * silencio si algun dia cambia el orden de las constantes, y este historial debe
 * seguir siendo legible tras futuras versiones de la aplicacion.
 *
 * La imagen NO se guarda en la base de datos. Se almacena como archivo en el
 * almacenamiento interno y aqui solo queda su nombre: meter fotografias en
 * SQLite hincharia la base y degradaria la lectura del historial.
 */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Momento de la revision, en milisegundos desde epoch (UTC). */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** Nombre de la constante de PodCondition. */
    @ColumnInfo(name = "condition")
    val condition: String,

    /** Confianza en el rango [0, 1]. */
    @ColumnInfo(name = "confidence")
    val confidence: Float,

    /** Tiempo de inferencia medido en el dispositivo (evidencia de RNF01). */
    @ColumnInfo(name = "inference_time_ms")
    val inferenceTimeMs: Long,

    /** Nombre de la constante de ClassificationOrigin: modelo real o simulacion. */
    @ColumnInfo(name = "origin")
    val origin: String,

    /** Identificador del modelo que produjo el resultado, para trazabilidad. */
    @ColumnInfo(name = "model_version")
    val modelVersion: String?,

    /** Nombre del archivo de imagen en el almacenamiento interno. */
    @ColumnInfo(name = "image_file_name")
    val imageFileName: String?,
)
