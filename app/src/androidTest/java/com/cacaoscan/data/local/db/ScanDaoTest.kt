package com.cacaoscan.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas de la capa de persistencia (RF06, RNF05).
 *
 * Se ejecutan sobre una base de datos Room real en memoria: comprueban las
 * consultas SQL de verdad, algo que un doble de prueba no puede validar.
 *
 * Ejecucion: requiere un dispositivo o emulador conectado.
 *   gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ScanDaoTest {

    private lateinit var database: CacaoScanDatabase
    private lateinit var dao: ScanDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CacaoScanDatabase::class.java,
        ).build()
        dao = database.scanDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(createdAt: Long, condition: String = "HEALTHY", image: String? = null) =
        ScanEntity(
            createdAt = createdAt,
            condition = condition,
            confidence = 0.9f,
            inferenceTimeMs = 250,
            origin = "SIMULATION",
            modelVersion = "demo",
            imageFileName = image,
        )

    @Test
    fun insertaYRecuperaUnRegistro() = runBlocking {
        val id = dao.insert(entity(createdAt = 1_000L, condition = "MONILIA_AFFECTED"))

        val stored = dao.getById(id)

        assertEquals("MONILIA_AFFECTED", stored?.condition)
        assertEquals(1_000L, stored?.createdAt)
    }

    @Test
    fun elHistorialSeOrdenaDelMasRecienteAlMasAntiguo() = runBlocking {
        dao.insert(entity(createdAt = 1_000L))
        dao.insert(entity(createdAt = 3_000L))
        dao.insert(entity(createdAt = 2_000L))

        val rows = dao.observeAll().first()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), rows.map { it.createdAt })
    }

    @Test
    fun eliminaUnRegistroConcreto() = runBlocking {
        val id = dao.insert(entity(createdAt = 1_000L))

        dao.deleteById(id)

        assertNull(dao.getById(id))
    }

    @Test
    fun devuelveElNombreDeArchivoParaPoderBorrarLaImagen() = runBlocking {
        val id = dao.insert(entity(createdAt = 1_000L, image = "foto.jpg"))

        assertEquals("foto.jpg", dao.getImageFileName(id))
        assertEquals(listOf("foto.jpg"), dao.getAllImageFileNames())
    }

    @Test
    fun vaciaTodoElHistorial() = runBlocking {
        dao.insert(entity(createdAt = 1_000L))
        dao.insert(entity(createdAt = 2_000L))

        dao.deleteAll()

        assertTrue(dao.observeAll().first().isEmpty())
    }
}
