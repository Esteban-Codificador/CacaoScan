package com.cacaoscan.presentation.history

import com.cacaoscan.data.repository.ScanRepositoryImpl
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.domain.usecase.DeleteAllScansUseCase
import com.cacaoscan.domain.usecase.GetScanHistoryUseCase
import com.cacaoscan.domain.usecase.ResolveScanImageUseCase
import com.cacaoscan.presentation.common.UiState
import com.cacaoscan.security.SecuritySession
import com.cacaoscan.security.authz.LocalTokenService
import com.cacaoscan.security.authz.TokenSigner
import com.cacaoscan.security.authz.constantTimeEquals
import com.cacaoscan.security.lock.AppLock
import com.cacaoscan.security.lock.AuthenticationResult
import com.cacaoscan.security.lock.LockAvailability
import com.cacaoscan.testing.FakeImageStore
import com.cacaoscan.testing.FakeScanDao
import com.cacaoscan.testing.classificationOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    /**
     * Un unico dispatcher para el Main, el repositorio y runTest: asi todas las
     * corrutinas comparten planificador y el estado se emite de forma inmediata.
     */
    private val dispatcher = UnconfinedTestDispatcher()
    private val dao = FakeScanDao()
    private val imageStore = FakeImageStore()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-05T15:30:00Z"), ZoneOffset.UTC)

    private val repository = ScanRepositoryImpl(dao, imageStore, dispatcher, clock)
    private val appLock = FakeAppLock()
    private val securitySession = SecuritySession(
        LocalTokenService(TestSigner(), clock),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HistoryViewModel(
        getScanHistory = GetScanHistoryUseCase(repository),
        resolveScanImage = ResolveScanImageUseCase(repository),
        deleteAllScans = DeleteAllScansUseCase(repository),
        appLock = appLock,
        securitySession = securitySession,
    )

    @Test
    fun `un historial sin registros se expone como estado vacio y no como error`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect { } }

            assertTrue(
                "El historial vacio no es un fallo",
                viewModel.uiState.value is UiState.Empty,
            )
        }

    @Test
    fun `mapea los registros a filas listas para mostrar`() = runTest(dispatcher) {
        repository.save(
            classificationOutput(
                condition = PodCondition.MONILIA_AFFECTED,
                confidenceFraction = 0.91f,
            ),
            image = null,
        )

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect { } }

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val items = (state as UiState.Success).data
        assertEquals(1, items.size)
        assertEquals("Senales de monilia", items[0].conditionLabel)
        assertEquals(91, items[0].confidencePercent)
        assertTrue("El registro proviene del clasificador de demostracion", items[0].isSimulated)
    }

    @Test
    fun `no borra el historial si la autenticacion falla`() = runTest(dispatcher) {
        repository.save(classificationOutput(), image = null)
        appLock.nextResult = AuthenticationResult.Cancelled

        viewModel().purgeHistory()

        assertEquals(
            "El historial debe seguir intacto",
            1,
            repository.observeHistory().first().size,
        )
    }

    @Test
    fun `no borra el historial si el dispositivo no puede autenticar`() = runTest(dispatcher) {
        repository.save(classificationOutput(), image = null)
        appLock.availability = LockAvailability.NOT_ENROLLED

        val viewModel = viewModel()
        viewModel.purgeHistory()

        assertEquals(1, repository.observeHistory().first().size)
        assertFalse("No debe intentar autenticar siquiera", appLock.authenticateCalled)
    }

    @Test
    fun `borra el historial tras autenticarse correctamente`() = runTest(dispatcher) {
        repository.save(classificationOutput(), image = null)
        appLock.nextResult = AuthenticationResult.Succeeded

        viewModel().purgeHistory()

        assertTrue(repository.observeHistory().first().isEmpty())
        assertTrue("Las imagenes tambien deben borrarse", imageStore.deletedAll)
        assertFalse(
            "La sesion autorizada no debe quedar abierta tras la accion",
            securitySession.isOpen,
        )
    }

    private class FakeAppLock : AppLock {
        var availability: LockAvailability = LockAvailability.AVAILABLE
        var nextResult: AuthenticationResult = AuthenticationResult.Succeeded
        var authenticateCalled: Boolean = false
            private set

        override fun availability(): LockAvailability = availability

        override suspend fun authenticate(reason: String): AuthenticationResult {
            authenticateCalled = true
            return nextResult
        }
    }

    private class TestSigner : TokenSigner {
        private val key = SecretKeySpec("clave-de-prueba".toByteArray(), "HmacSHA256")
        override val algorithm: String = "HS256"
        override fun sign(content: ByteArray): ByteArray =
            Mac.getInstance("HmacSHA256").apply { init(key) }.doFinal(content)

        override fun verify(content: ByteArray, signature: ByteArray): Boolean =
            constantTimeEquals(sign(content), signature)
    }
}
