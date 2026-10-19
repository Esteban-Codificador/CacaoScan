package com.cacaoscan.testing

import android.graphics.Bitmap
import com.cacaoscan.data.local.db.ScanDao
import com.cacaoscan.data.local.db.ScanEntity
import com.cacaoscan.data.local.file.ImageStore
import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.model.Confidence
import com.cacaoscan.domain.model.PodCondition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Dobles de prueba compartidos.
 *
 * Son implementaciones en memoria de las mismas interfaces que usa la
 * aplicacion, no simulacros generados: mantienen el comportamiento observable
 * (orden del historial, borrado en cascada) y permiten probar el repositorio sin
 * dispositivo ni base de datos real.
 */

class FakeScanDao : ScanDao {

    private val rows = MutableStateFlow<List<ScanEntity>>(emptyList())
    private var nextId = 1L

    /** Cuando es true, insert lanza: sirve para probar el camino de error. */
    var failOnInsert: Boolean = false

    override fun observeAll(): Flow<List<ScanEntity>> =
        rows.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun getById(id: Long): ScanEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(scan: ScanEntity): Long {
        if (failOnInsert) throw IllegalStateException("fallo simulado de escritura")
        val id = nextId++
        rows.value = rows.value + scan.copy(id = id)
        return id
    }

    override suspend fun getImageFileName(id: Long): String? =
        rows.value.firstOrNull { it.id == id }?.imageFileName

    override suspend fun getAllImageFileNames(): List<String> =
        rows.value.mapNotNull { it.imageFileName }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

class FakeImageStore : ImageStore {

    val saved = mutableListOf<String>()
    val deleted = mutableListOf<String?>()
    var deletedAll: Boolean = false
        private set

    /** Cuando es true, save lanza IOException: la revision debe guardarse sin foto. */
    var failOnSave: Boolean = false

    private var counter = 0

    override fun save(bitmap: Bitmap): String {
        if (failOnSave) throw java.io.IOException("fallo simulado de almacenamiento")
        val name = "imagen-${counter++}.jpg"
        saved += name
        return name
    }

    override fun resolvePath(fileName: String?): String? =
        fileName?.takeIf { it in saved }?.let { "/fake/$it" }

    override fun delete(fileName: String?) {
        deleted += fileName
        saved.remove(fileName)
    }

    override fun deleteAll() {
        deletedAll = true
        saved.clear()
    }
}

/** Construye un resultado de clasificacion para las pruebas. */
fun classificationOutput(
    condition: PodCondition = PodCondition.MONILIA_AFFECTED,
    confidenceFraction: Float = 0.9f,
    inferenceTimeMs: Long = 300,
    origin: ClassificationOrigin = ClassificationOrigin.SIMULATION,
    modelVersion: String? = "demo-sin-modelo",
): ClassificationOutput {
    val confidence = Confidence.ofFraction(confidenceFraction)
    val other = Confidence.ofFraction(1f - confidenceFraction)
    val probabilities = PodCondition.entries.associateWith { entry ->
        if (entry == condition) confidence else other
    }
    return ClassificationOutput(
        condition = condition,
        confidence = confidence,
        probabilities = probabilities,
        inferenceTimeMs = inferenceTimeMs,
        origin = origin,
        modelVersion = modelVersion,
    )
}
