package com.cacaoscan.data.mapper

import com.cacaoscan.data.local.db.ScanEntity
import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.Confidence
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.domain.model.ScanRecord
import java.time.Instant

/**
 * Conversion entre la fila de Room y el modelo de dominio.
 *
 * Los enumerados se leen de forma tolerante: si una fila quedara con un valor
 * desconocido tras una futura migracion, el historial debe seguir abriendo en
 * lugar de lanzar una excepcion en mitad de la lista.
 */
fun ScanEntity.toDomain(): ScanRecord = ScanRecord(
    id = id,
    createdAt = Instant.ofEpochMilli(createdAt),
    condition = condition.toPodCondition(),
    confidence = Confidence.ofFraction(confidence),
    inferenceTimeMs = inferenceTimeMs,
    origin = origin.toClassificationOrigin(),
    modelVersion = modelVersion,
    imageFileName = imageFileName,
)

fun ScanRecord.toEntity(): ScanEntity = ScanEntity(
    id = id,
    createdAt = createdAt.toEpochMilli(),
    condition = condition.name,
    confidence = confidence.fraction,
    inferenceTimeMs = inferenceTimeMs,
    origin = origin.name,
    modelVersion = modelVersion,
    imageFileName = imageFileName,
)

private fun String.toPodCondition(): PodCondition =
    PodCondition.entries.firstOrNull { it.name == this } ?: PodCondition.MONILIA_AFFECTED

/**
 * Ante un origen desconocido se asume SIMULATION. Es el valor conservador: es
 * preferible marcar de mas un resultado como simulado que presentar como real
 * uno cuya procedencia no se puede confirmar.
 */
private fun String.toClassificationOrigin(): ClassificationOrigin =
    ClassificationOrigin.entries.firstOrNull { it.name == this } ?: ClassificationOrigin.SIMULATION
