package com.cacaoscan.data.repository

import com.cacaoscan.domain.model.ClassificationOrigin
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.testing.FakeImageStore
import com.cacaoscan.testing.FakeScanDao
import com.cacaoscan.testing.classificationOutput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ScanRepositoryImplTest {

    private val fixedInstant: Instant = Instant.parse("2026-09-05T15:30:00Z")
    private val dao = FakeScanDao()
    private val imageStore = FakeImageStore()

    /**
     * Un unico dispatcher de prueba, compartido por el repositorio y por runTest.
     * Si el repositorio usara otro, ambos tendrian planificadores distintos y las
     * corrutinas del repositorio nunca llegarian a ejecutarse.
     */
    private val testDispatcher = StandardTestDispatcher()

    private fun repository(clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)) =
        ScanRepositoryImpl(
            scanDao = dao,
            imageStore = imageStore,
            ioDispatcher = testDispatcher,
            clock = clock,
        )

    @Test
    fun `guarda la clasificacion con su fecha, categoria y confianza`() = runTest(testDispatcher) {
        val repository = repository()

        val id = repository.save(
            classificationOutput(condition = PodCondition.MONILIA_AFFECTED, confidenceFraction = 0.87f),
            image = null,
        )

        val stored = repository.getById(id)
        assertNotNull(stored)
        assertEquals(PodCondition.MONILIA_AFFECTED, stored!!.condition)
        assertEquals(87, stored.confidence.percent)
        assertEquals(fixedInstant, stored.createdAt)
    }

    @Test
    fun `conserva la marca de resultado simulado`() = runTest(testDispatcher) {
        val repository = repository()

        val id = repository.save(
            classificationOutput(origin = ClassificationOrigin.SIMULATION),
            image = null,
        )

        assertTrue(repository.getById(id)!!.isSimulated)
    }

    @Test
    fun `el historial se entrega del mas reciente al mas antiguo`() = runTest(testDispatcher) {
        val first = repository(Clock.fixed(fixedInstant, ZoneOffset.UTC))
        first.save(classificationOutput(condition = PodCondition.HEALTHY), image = null)

        val later = repository(Clock.fixed(fixedInstant.plusSeconds(60), ZoneOffset.UTC))
        later.save(classificationOutput(condition = PodCondition.MONILIA_AFFECTED), image = null)

        val history = later.observeHistory().first()

        assertEquals(2, history.size)
        assertEquals(PodCondition.MONILIA_AFFECTED, history[0].condition)
        assertEquals(PodCondition.HEALTHY, history[1].condition)
    }

    @Test
    fun `al eliminar una revision tambien se borra su imagen`() = runTest(testDispatcher) {
        val repository = repository()
        val id = dao.insert(
            com.cacaoscan.data.local.db.ScanEntity(
                createdAt = fixedInstant.toEpochMilli(),
                condition = PodCondition.HEALTHY.name,
                confidence = 0.9f,
                inferenceTimeMs = 100,
                origin = ClassificationOrigin.SIMULATION.name,
                modelVersion = "demo",
                imageFileName = "imagen-0.jpg",
            ),
        )

        repository.delete(id)

        assertNull(repository.getById(id))
        assertTrue(imageStore.deleted.contains("imagen-0.jpg"))
    }

    @Test
    fun `borrar todo el historial borra tambien todas las imagenes`() = runTest(testDispatcher) {
        val repository = repository()
        repository.save(classificationOutput(), image = null)

        repository.deleteAll()

        assertTrue(repository.observeHistory().first().isEmpty())
        assertTrue(imageStore.deletedAll)
    }

    @Test
    fun `si falla la escritura de la fila no queda una imagen huerfana`() = runTest(testDispatcher) {
        val repository = repository()
        dao.failOnInsert = true

        runCatching { repository.save(classificationOutput(), image = null) }

        assertTrue("No debe quedar ninguna imagen guardada", imageStore.saved.isEmpty())
    }

    @Test
    fun `resolveImagePath devuelve null cuando la imagen ya no existe`() = runTest(testDispatcher) {
        val repository = repository()
        assertNull(repository.resolveImagePath("inexistente.jpg"))
        assertNull(repository.resolveImagePath(null))
    }
}
