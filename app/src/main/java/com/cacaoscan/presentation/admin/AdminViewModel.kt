package com.cacaoscan.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.domain.model.admin.EstadoServicio
import com.cacaoscan.domain.model.admin.ServicioRemoto
import com.cacaoscan.domain.repository.MicroserviciosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val denegado: String? = null,
    val anfitrion: String = "",
    val anfitrionEditado: String = "",
    val errorAnfitrion: String? = null,
    val comprobando: Boolean = false,
    val estados: List<EstadoServicio> = emptyList(),
)

/**
 * Entrada al modulo de administracion: donde estan los microservicios y si
 * responden. Es lo primero que se mira cuando algo no funciona.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repositorio: MicroserviciosRepository,
    private val autorizacion: AutorizacionAdmin,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(anfitrion = repositorio.anfitrion, anfitrionEditado = repositorio.anfitrion),
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        comprobar()
    }

    fun comprobar() {
        autorizacion.puedeLeer()?.let { motivo ->
            _uiState.update { it.copy(denegado = motivo) }
            return
        }
        _uiState.update { it.copy(denegado = null, comprobando = true) }
        viewModelScope.launch {
            // Los dos servicios se consultan a la vez: si uno esta caido, su
            // tiempo de espera no retrasa la respuesta del otro.
            val estados = ServicioRemoto.entries
                .map { servicio -> async { repositorio.estado(servicio) } }
                .awaitAll()
            _uiState.update { it.copy(comprobando = false, estados = estados) }
        }
    }

    fun editarAnfitrion(valor: String) {
        _uiState.update { it.copy(anfitrionEditado = valor, errorAnfitrion = null) }
    }

    fun guardarAnfitrion() {
        val guardado = repositorio.cambiarAnfitrion(_uiState.value.anfitrionEditado)
        if (guardado == null) {
            _uiState.update {
                it.copy(errorAnfitrion = "No es una direccion valida. Ejemplos: 10.0.2.2 o 192.168.1.20")
            }
            return
        }
        _uiState.update { it.copy(anfitrion = guardado, anfitrionEditado = guardado, errorAnfitrion = null) }
        comprobar()
    }
}
