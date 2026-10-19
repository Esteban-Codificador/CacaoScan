package com.cacaoscan.data.repository

import android.graphics.Bitmap
import com.cacaoscan.data.local.db.ScanDao
import com.cacaoscan.data.local.db.ScanEntity
import com.cacaoscan.data.local.file.ImageStore
import com.cacaoscan.data.mapper.toDomain
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.model.ScanRecord
import com.cacaoscan.domain.repository.ScanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock

/**
 * Implementacion del historial sobre Room y almacenamiento interno.
 *
 * Coordina las dos mitades del registro: la fila en la base de datos y el
 * archivo de imagen. Si la imagen no se puede guardar, la revision se guarda
 * igualmente sin imagen: perder la foto es molesto, perder el resultado de la
 * revision lo es mas.
 *
 * [Clock] se inyecta en lugar de llamar a Instant.now() para que las pruebas
 * puedan fijar el tiempo.
 */
class ScanRepositoryImpl(
    private val scanDao: ScanDao,
    private val imageStore: ImageStore,
    private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
) : ScanRepository {

    override fun observeHistory(): Flow<List<ScanRecord>> =
        scanDao.observeAll().map { entities -> entities.map(ScanEntity::toDomain) }

    override suspend fun getById(id: Long): ScanRecord? = withContext(ioDispatcher) {
        scanDao.getById(id)?.toDomain()
    }

    override suspend fun save(output: ClassificationOutput, image: Bitmap?): Long =
        withContext(ioDispatcher) {
            val fileName = image?.let { bitmap ->
                runCatching { imageStore.save(bitmap) }
                    .getOrElse { error ->
                        if (error is IOException) null else throw error
                    }
            }

            val entity = ScanEntity(
                createdAt = clock.instant().toEpochMilli(),
                condition = output.condition.name,
                confidence = output.confidence.fraction,
                inferenceTimeMs = output.inferenceTimeMs,
                origin = output.origin.name,
                modelVersion = output.modelVersion,
                imageFileName = fileName,
            )

            try {
                scanDao.insert(entity)
            } catch (error: Throwable) {
                // La fila no se creo: no se deja la imagen huerfana ocupando disco.
                imageStore.delete(fileName)
                throw error
            }
        }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        val fileName = scanDao.getImageFileName(id)
        scanDao.deleteById(id)
        imageStore.delete(fileName)
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        scanDao.deleteAll()
        imageStore.deleteAll()
    }

    override fun resolveImagePath(fileName: String?): String? = imageStore.resolvePath(fileName)
}
