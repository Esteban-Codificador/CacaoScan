package com.cacaoscan.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cacaoscan.domain.model.admin.ModeloRegistrado
import com.cacaoscan.domain.model.admin.NuevoModelo
import com.cacaoscan.domain.model.admin.ResultadoRemoto
import com.cacaoscan.domain.model.admin.ResumenRegistro
import com.cacaoscan.domain.repository.MicroserviciosRepository
import com.cacaoscan.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

data class DatosRegistro(
    val resumen: ResumenRegistro,
    val modelos: List<ModeloRegistrado>,
)

/** Campos del formulario tal como se escriben: la conversion se hace al enviar. */
data class FormularioModelo(
    val version: String = "",
    val exactitud: String = "",
    val sensibilidad: String = "",
    val hash: String = "",
    val tamanoMb: String = "",
    val notas: String = "",
)

data class ModelosUiState(
    val denegado: String? = null,
    val carga: UiState<DatosRegistro> = UiState.Loading("Consultando el Registro de Modelos..."),
    val formulario: FormularioModelo = FormularioModelo(),
    val enviando: Boolean = false,
    /** Motivos de rechazo devueltos por el validador del microservicio. */
    val erroresEnvio: List<String> = emptyList(),
    val ultimoRegistrado: ModeloRegistrado? = null,
)

/**
 * Microservicio 1 desde la app: consultar y registrar modelos.
 *
 * La app NO valida las reglas de negocio (version semantica, rango de las
 * metricas, hash, 100 MB, umbral del 85 %). Las decide el Script del Validador
 * del microservicio y aqui solo se muestran sus motivos. Asi lo que se ve en la
 * app es exactamente lo que hace el servicio.
 */
@HiltViewModel
class ModelosViewModel @Inject constructor(
    private val repositorio: MicroserviciosRepository,
    private val autorizacion: AutorizacionAdmin,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelosUiState())
    val uiState: StateFlow<ModelosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        autorizacion.puedeLeer()?.let { motivo ->
            _uiState.update { it.copy(denegado = motivo) }
            return
        }
        _uiState.update { it.copy(denegado = null, carga = UiState.Loading("Consultando el Registro de Modelos...")) }
        viewModelScope.launch {
            val resumen = async { repositorio.resumenRegistro() }
            val modelos = async { repositorio.listarModelos() }
            val r = resumen.await()
            val m = modelos.await()

            val carga = if (r is ResultadoRemoto.Exito && m is ResultadoRemoto.Exito) {
                if (m.datos.isEmpty()) {
                    UiState.Empty("Todavia no hay modelos registrados. Registre el primero abajo.")
                } else {
                    // Mas reciente primero: es la que interesa al publicar.
                    UiState.Success(DatosRegistro(r.datos, m.datos.reversed()))
                }
            } else {
                UiState.Error((r.mensajeDeFallo() ?: m.mensajeDeFallo()).orEmpty())
            }
            _uiState.update { it.copy(carga = carga) }
        }
    }

    fun editar(formulario: FormularioModelo) {
        _uiState.update { it.copy(formulario = formulario, erroresEnvio = emptyList()) }
    }

    fun registrar() {
        autorizacion.puedeEscribir()?.let { motivo ->
            _uiState.update { it.copy(erroresEnvio = listOf(motivo)) }
            return
        }
        val f = _uiState.value.formulario
        val nuevo = convertir(f) ?: return

        _uiState.update { it.copy(enviando = true, erroresEnvio = emptyList(), ultimoRegistrado = null) }
        viewModelScope.launch {
            when (val resultado = repositorio.registrarModelo(nuevo)) {
                is ResultadoRemoto.Exito -> {
                    _uiState.update {
                        it.copy(enviando = false, ultimoRegistrado = resultado.datos, formulario = FormularioModelo())
                    }
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

    /*
     * Ejemplos para demostrar las tres respuestas posibles del servicio. Las
     * cifras son de DEMOSTRACION y asi se anotan en las notas del modelo: no son
     * resultados de ningun entrenamiento.
     */

    fun ejemploApto() = editar(
        FormularioModelo(
            version = siguienteVersion(),
            exactitud = "0.912",
            sensibilidad = "0.934",
            hash = hashAleatorio(),
            tamanoMb = "4.7",
            notas = "Datos de demostracion, no es un entrenamiento real",
        ),
    )

    /** Se registra, pero el servicio lo marca como no apto: no alcanza el 85 % (RNF04). */
    fun ejemploBajoUmbral() = editar(
        FormularioModelo(
            version = siguienteVersion(),
            exactitud = "0.812",
            sensibilidad = "0.790",
            hash = hashAleatorio(),
            tamanoMb = "4.7",
            notas = "Datos de demostracion: exactitud por debajo del 85 %",
        ),
    )

    /** El validador lo rechaza entero: version, metricas, hash y tamano incorrectos. */
    fun ejemploInvalido() = editar(
        FormularioModelo(
            version = "1.0",
            exactitud = "1.4",
            sensibilidad = "-0.2",
            hash = "abc123",
            tamanoMb = "250",
            notas = "Datos de demostracion: deben ser rechazados",
        ),
    )

    private fun siguienteVersion(): String {
        val modelos = (_uiState.value.carga as? UiState.Success)?.data?.modelos.orEmpty()
        return siguienteVersion(modelos.map { it.version })
    }

    private fun convertir(f: FormularioModelo): NuevoModelo? {
        val exactitud = decimal(f.exactitud)
        val sensibilidad = decimal(f.sensibilidad)
        val tamano = decimal(f.tamanoMb)
        // Solo se comprueba que sean numeros: el rango lo decide el servicio.
        val faltan = buildList {
            if (f.version.isBlank()) add("version: es obligatoria")
            if (exactitud == null) add("exactitud: escriba un numero, por ejemplo 0.912")
            if (sensibilidad == null) add("sensibilidad: escriba un numero, por ejemplo 0.934")
            if (tamano == null) add("tamano_mb: escriba un numero, por ejemplo 4.7")
        }
        if (faltan.isNotEmpty()) {
            _uiState.update { it.copy(erroresEnvio = faltan) }
            return null
        }
        return NuevoModelo(
            version = f.version.trim(),
            exactitud = exactitud!!,
            sensibilidad = sensibilidad!!,
            hashSha256 = f.hash.trim(),
            tamanoMb = tamano!!,
            notas = f.notas.trim(),
        )
    }

    companion object {
        /** Acepta coma decimal: es lo que escribe un teclado configurado en espanol. */
        fun decimal(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

        /**
         * Version siguiente a la mayor registrada, subiendo el parche.
         * Las versiones son inmutables en el servicio, asi que repetir una se rechaza.
         */
        fun siguienteVersion(existentes: List<String>): String {
            val mayor = existentes.mapNotNull { v ->
                v.split('.').takeIf { it.size == 3 }?.map { it.toIntOrNull() ?: return@mapNotNull null }
            }.maxWithOrNull(compareBy<List<Int>>({ it[0] }, { it[1] }, { it[2] }))
                ?: return "0.1.0"
            return "${mayor[0]}.${mayor[1]}.${mayor[2] + 1}"
        }

        private val aleatorio = SecureRandom()

        private fun hashAleatorio(): String {
            val bytes = ByteArray(32).also(aleatorio::nextBytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
