package com.cacaoscan.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.domain.model.admin.ModeloRegistrado
import com.cacaoscan.domain.model.admin.ResumenRegistro
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.PrimaryAction
import com.cacaoscan.presentation.common.UiState

@Composable
fun ModelosScreen(
    viewModel: ModelosViewModel,
    onNavigateBack: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    CacaoScreen(
        title = "Registro de Modelos",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = viewModel::cargar) {
                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
            }
        },
    ) { contentModifier ->
        val motivo = estado.denegado
        if (motivo != null) {
            AccesoDenegado(motivo = motivo, onIniciarSesion = onIniciarSesion, modifier = contentModifier)
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = contentModifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "Microservicio 1 · puerto 8001",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                when (val carga = estado.carga) {
                    is UiState.Loading -> Text(carga.message.orEmpty())
                    is UiState.Error -> TarjetaSeccion(titulo = "No se pudo consultar") {
                        ListaMensajes(listOf(carga.message))
                    }
                    is UiState.Empty -> TarjetaSeccion(titulo = "Registro vacio") { Text(carga.message) }
                    is UiState.Success -> {
                        TarjetaResumen(carga.data.resumen)
                        TarjetaSeccion(titulo = "Modelos registrados (${carga.data.modelos.size})") {
                            carga.data.modelos.forEachIndexed { i, modelo ->
                                if (i > 0) HorizontalDivider()
                                FilaModelo(modelo)
                            }
                        }
                    }
                }

                FormularioRegistro(estado, viewModel)
            }
        }
    }
}

@Composable
private fun TarjetaResumen(resumen: ResumenRegistro) {
    TarjetaSeccion(titulo = "Resumen  ·  GET /resumen") {
        FilaDato("Version vigente", resumen.versionVigente ?: "ninguna apta todavia")
        FilaDato("Registrados", resumen.totalRegistrados.toString())
        FilaDato("Aptos para publicar", resumen.aptosParaPublicar.toString())
        FilaDato("No aptos (< 85 %)", resumen.noAptos.toString())
        FilaDato("Mejor exactitud", resumen.mejorExactitud?.let(::porcentaje) ?: "-")
    }
}

@Composable
private fun FilaModelo(modelo: ModeloRegistrado) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("v${modelo.version}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (modelo.aptoParaPublicar) InsigniaPositiva("APTO") else InsigniaNegativa("NO APTO")
        }
        Text(
            text = "Exactitud ${porcentaje(modelo.exactitud)}  ·  Sensibilidad ${porcentaje(modelo.sensibilidad)}  ·  " +
                "${modelo.tamanoMb} MB",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = modelo.motivo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (modelo.notas.isNotBlank()) {
            Text(modelo.notas, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioRegistro(estado: ModelosUiState, viewModel: ModelosViewModel) {
    val f = estado.formulario
    TarjetaSeccion(
        titulo = "Registrar un modelo  ·  POST /modelos",
        subtitulo = "Las reglas las aplica el Script del Validador del microservicio, no la app.",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = viewModel::ejemploApto, label = { Text("Ejemplo apto") })
            AssistChip(onClick = viewModel::ejemploBajoUmbral, label = { Text("Ejemplo bajo 85 %") })
            AssistChip(onClick = viewModel::ejemploInvalido, label = { Text("Ejemplo invalido") })
        }

        Campo("Version (X.Y.Z)", f.version) { viewModel.editar(f.copy(version = it)) }
        Campo("Exactitud (0 a 1)", f.exactitud, numerico = true) { viewModel.editar(f.copy(exactitud = it)) }
        Campo("Sensibilidad (0 a 1)", f.sensibilidad, numerico = true) { viewModel.editar(f.copy(sensibilidad = it)) }
        Campo("SHA-256 del .tflite", f.hash) { viewModel.editar(f.copy(hash = it)) }
        Campo("Tamano (MB)", f.tamanoMb, numerico = true) { viewModel.editar(f.copy(tamanoMb = it)) }
        Campo("Notas", f.notas) { viewModel.editar(f.copy(notas = it)) }

        if (estado.erroresEnvio.isNotEmpty()) {
            Text("El servicio rechazo el modelo:", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error)
            ListaMensajes(estado.erroresEnvio)
        }
        estado.ultimoRegistrado?.let { m ->
            Text(
                text = "Registrado v${m.version}: ${m.motivo}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (m.aptoParaPublicar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }

        PrimaryAction(
            text = if (estado.enviando) "Enviando..." else "Registrar modelo",
            icon = Icons.Default.CloudUpload,
            onClick = viewModel::registrar,
            enabled = !estado.enviando,
        )
    }
}

@Composable
internal fun Campo(
    etiqueta: String,
    valor: String,
    numerico: Boolean = false,
    onCambio: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth(),
    )
}
