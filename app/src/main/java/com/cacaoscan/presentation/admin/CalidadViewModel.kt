package com.cacaoscan.presentation.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.data.local.file.LectorMetadatosImagen
import com.cacaoscan.domain.model.admin.ClaseAnotada
import com.cacaoscan.domain.model.admin.ImagenCandidata
import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.domain.model.admin.ResumenCalidad
import com.cacaoscan.domain.model.admin.ValidacionImagen
import com.cacaoscan.domain.repository.MicroserviciosRepository
import com.cacaoscan.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DatosCalidad(
    val resumen: ResumenCalidad,
    val historial: List<ValidacionImagen>,
)

data class FormularioImagen(
    val archivo: String = "",
    val ancho: String = "",
    val alto: String = "",
    val pesoKb: String = "",
    val clases: Set<ClaseAnotada> = emptySet(),
    /** true si los datos se leyeron de una foto del telefono y no se escribieron a mano. */
    val desdeFoto: Boolean = false,
)

data class CalidadUiState(
    val denegado: String? = null,
    val carga: UiState<DatosCalidad> = UiState.Loading("Consultando el Control de Calidad..."),
    val soloRechazadas: Boolean = false,
    val formulario: FormularioImagen = FormularioImagen(),
    val enviando: Boolean = false,
    val veredicto: ValidacionImagen? = null,
    val erroresEnvio: List<String> = emptyList(),
)

/**
 * Microservicio 2 desde la app: validar imagenes candidatas al conjunto de
 * entrenamiento y revisar el historial de decisiones.
 */
@HiltViewModel
class CalidadViewModel @Inject constructor(
    private val repositorio: MicroserviciosRepository,
    private val autorizacion: AutorizacionAdmin,
    private val lectorMetadatos: LectorMetadatosImagen,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalidadUiState())
    val uiState: StateFlow<CalidadUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        autorizacion.puedeLeer()?.let { motivo ->
            _uiState.update { it.copy(denegado = motivo) }
            return
        }
        _uiState.update { it.copy(denegado = null) }
        viewModelScope.launch {
            val resumen = async { repositorio.resumenCalidad() }
            val historial = async { repositorio.historialValidaciones(_uiState.value.soloRechazadas) }
            val r = resumen.await()
            val h = historial.await()
            val carga = if (r is ResultadoRemoto.Exito && h is ResultadoRemoto.Exito) {
                UiState.Success(DatosCalidad(r.datos, h.datos.reversed()))
            } else {
                UiState.Error((r.mensajeDeFallo() ?: h.mensajeDeFallo()).orEmpty())
            }
            _uiState.update { it.copy(carga = carga) }
        }
    }

    fun alternarSoloRechazadas() {
        _uiState.update { it.copy(soloRechazadas = !it.soloRechazadas) }
        cargar()
    }

    fun editar(formulario: FormularioImagen) {
        _uiState.update { it.copy(formulario = formulario, erroresEnvio = emptyList()) }
    }

    fun alternarClase(clase: ClaseAnotada) {
        val f = _uiState.value.formulario
        val clases = if (clase in f.clases) f.clases - clase else f.clases + clase
        editar(f.copy(clases = clases))
    }

    /** Rellena el formulario con los metadatos de una foto elegida en el telefono. */
    fun usarFoto(uri: Uri) {
        viewModelScope.launch {
            val datos = lectorMetadatos.leer(uri)
            if (datos == null) {
                _uiState.update { it.copy(erroresEnvio = listOf("No se pudo leer la imagen elegida.")) }
                return@launch
            }
            editar(
                _uiState.value.formulario.copy(
                    archivo = datos.nombre,
                    ancho = datos.ancho.toString(),
                    alto = datos.alto.toString(),
                    pesoKb = "%.1f".format(java.util.Locale.ROOT, datos.pesoKb),
                    desdeFoto = true,
                ),
            )
        }
    }

    fun validar() {
        autorizacion.puedeEscribir()?.let { motivo ->
            _uiState.update { it.copy(erroresEnvio = listOf(motivo)) }
            return
        }
        val imagen = convertir(_uiState.value.formulario) ?: return
        _uiState.update { it.copy(enviando = true, veredicto = null, erroresEnvio = emptyList()) }
        viewModelScope.launch {
            when (val resultado = repositorio.validarImagen(imagen)) {
                is ResultadoRemoto.Exito -> {
                    _uiState.update { it.copy(enviando = false, veredicto = resultado.datos) }
                    cargar()
                }
                is ResultadoRemoto.Rechazado ->
                    _uiState.update { it.copy(enviando = false, erroresEnvio = resultado.errores) }
                else ->
                    _uiState.update { it.copy(enviando = false, erroresEnvio = listOfNotNull(resultado.mensajeDeFallo())) }
            }
        }
    }

    // ------------------------------------------------------------ ejemplos

    fun ejemploValido() = editar(
        FormularioImagen("IMG_demo_vaina_m3.jpg", "3024", "3024", "2480.5", setOf(ClaseAnotada.M3)),
    )

    /**
     * El hallazgo de la fase de Comprension de los Datos: imagenes con vainas
     * sanas y afectadas a la vez no admiten una etiqueta unica. El servicio las rechaza.
     */
    fun ejemploAmbiguo() = editar(
        FormularioImagen("IMG_demo_mixta.jpg", "3024", "3024", "2390.0", setOf(ClaseAnotada.H0, ClaseAnotada.M2)),
    )

    fun ejemploDefectuoso() = editar(
        FormularioImagen("captura_pequena.bmp", "160", "120", "45.0", emptySet()),
    )

    private fun convertir(f: FormularioImagen): ImagenCandidata? {
        val ancho = f.ancho.trim().toIntOrNull()
        val alto = f.alto.trim().toIntOrNull()
        val peso = ModelosViewModel.decimal(f.pesoKb)
        val faltan = buildList {
            if (f.archivo.isBlank()) add("archivo: escriba el nombre, por ejemplo IMG_0001.jpg")
            if (ancho == null) add("ancho: escriba un numero entero de pixeles")
            if (alto == null) add("alto: escriba un numero entero de pixeles")
            if (peso == null) add("peso_kb: escriba un numero")
        }
        if (faltan.isNotEmpty()) {
            _uiState.update { it.copy(erroresEnvio = faltan) }
            return null
        }
        return ImagenCandidata(
            archivo = f.archivo.trim(),
            ancho = ancho!!,
            alto = alto!!,
            pesoKb = peso!!,
            clases = f.clases.sortedBy { it.ordinal }.map { it.codigo },
        )
    }
}
