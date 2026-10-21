package com.cacaoscan.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.domain.model.admin.MetricasServicio
import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.domain.repository.MicroserviciosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LecturaMonitor(
    val servicio: ServicioRemoto,
    val metricas: MetricasServicio?,
    val fallo: String?,
)

data class MonitorUiState(
    val denegado: String? = null,
    val lecturas: List<LecturaMonitor> = emptyList(),
    val siguiendo: Boolean = true,
    val ultimaLectura: String? = null,
    val generandoTrafico: Boolean = false,
    val mensaje: String? = null,
)

/**
 * Etapa MONITOR dentro de la app: el mismo panel que panel.py, leyendo
 * GET /metricas de ambos servicios cada 3 segundos mientras la pantalla esta abierta.
 *
 * La consulta periodica vive en viewModelScope, de modo que se detiene sola al
 * salir de la pantalla: no queda ningun sondeo en segundo plano gastando bateria.
 */
@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val repositorio: MicroserviciosRepository,
    private val autorizacion: AutorizacionAdmin,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var sondeo: Job? = null

    init {
        iniciarSondeo()
    }

    fun alternarSeguimiento() {
        if (_uiState.value.siguiendo) {
            sondeo?.cancel()
            _uiState.update { it.copy(siguiendo = false) }
        } else {
            iniciarSondeo()
        }
    }

    fun leerAhora() {
        viewModelScope.launch { leer() }
    }

    /**
     * Genera peticiones reales contra el Registro de Modelos, incluida una que
     * falla a proposito (404), para ver en vivo como cambian las metricas.
     * Es lo mismo que hace 'python demostracion.py monitor'.
     */
    fun generarTrafico() {
        autorizacion.puedeLeer()?.let { motivo ->
            _uiState.update { it.copy(denegado = motivo) }
            return
        }
        _uiState.update { it.copy(generandoTrafico = true, mensaje = null) }
        viewModelScope.launch {
            repeat(PETICIONES_DE_PRUEBA) { repositorio.listarModelos() }
            repositorio.obtenerModelo(VERSION_INEXISTENTE)
            leer()
            _uiState.update {
                it.copy(
                    generandoTrafico = false,
                    mensaje = "Enviadas $PETICIONES_DE_PRUEBA peticiones correctas y 1 que devuelve 404.",
                )
            }
        }
    }

    private fun iniciarSondeo() {
        sondeo?.cancel()
        _uiState.update { it.copy(siguiendo = true) }
        sondeo = viewModelScope.launch {
            while (isActive) {
                leer()
                delay(INTERVALO_MS)
            }
        }
    }

    private suspend fun leer() {
        autorizacion.puedeLeer()?.let { motivo ->
            sondeo?.cancel()
            _uiState.update { it.copy(denegado = motivo, siguiendo = false) }
            return
        }
        val lecturas = coroutineScope {
            ServicioRemoto.entries.map { servicio ->
                async {
                    when (val r = repositorio.metricas(servicio)) {
                        is ResultadoRemoto.Exito -> LecturaMonitor(servicio, r.datos, null)
                        else -> LecturaMonitor(servicio, null, r.mensajeDeFallo())
                    }
                }
            }.awaitAll()
        }
        _uiState.update {
            it.copy(
                denegado = null,
                lecturas = lecturas,
                ultimaLectura = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            )
        }
    }

    private companion object {
        const val INTERVALO_MS = 3_000L
        const val PETICIONES_DE_PRUEBA = 5
        const val VERSION_INEXISTENTE = "9.9.9"
    }
}
