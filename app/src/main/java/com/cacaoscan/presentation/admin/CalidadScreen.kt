package com.cacaoscan.presentation.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.domain.model.admin.ClaseAnotada
import com.cacaoscan.domain.model.admin.ResumenCalidad
import com.cacaoscan.domain.model.admin.ValidacionImagen
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.PrimaryAction
import com.cacaoscan.presentation.common.SecondaryAction
import com.cacaoscan.presentation.common.UiState

@Composable
fun CalidadScreen(
    viewModel: CalidadViewModel,
    onNavigateBack: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Selector de Fotos del sistema: no requiere permisos de almacenamiento.
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.usarFoto(uri)
    }

    CacaoScreen(
        title = "Control de Calidad",
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
                    text = "Microservicio 2 · puerto 8002",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                FormularioValidacion(
                    estado = estado,
                    viewModel = viewModel,
                    onElegirFoto = {
                        selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )

                estado.veredicto?.let { TarjetaVeredicto(it) }

                when (val carga = estado.carga) {
                    is UiState.Loading -> Text(carga.message.orEmpty())
                    is UiState.Error -> TarjetaSeccion(titulo = "No se pudo consultar") {
                        ListaMensajes(listOf(carga.message))
                    }
                    is UiState.Empty -> Text(carga.message)
                    is UiState.Success -> {
                        TarjetaResumenCalidad(carga.data.resumen)
                        TarjetaHistorial(carga.data.historial, estado.soloRechazadas, viewModel::alternarSoloRechazadas)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioValidacion(
    estado: CalidadUiState,
    viewModel: CalidadViewModel,
    onElegirFoto: () -> Unit,
) {
    val f = estado.formulario
    TarjetaSeccion(
        titulo = "Validar una imagen  ·  POST /validaciones",
        subtitulo = "Solo se envian nombre, dimensiones, peso y clases. La fotografia no sale del telefono.",
    ) {
        SecondaryAction(text = "Usar una foto del telefono", icon = Icons.Default.PhotoLibrary, onClick = onElegirFoto)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = viewModel::ejemploValido, label = { Text("Ejemplo valido") })
            AssistChip(onClick = viewModel::ejemploAmbiguo, label = { Text("Ejemplo ambiguo") })
            AssistChip(onClick = viewModel::ejemploDefectuoso, label = { Text("Ejemplo defectuoso") })
        }
        if (f.desdeFoto) {
            Text(
                "Datos leidos de la foto elegida. Anote abajo las clases que contiene.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Campo("Archivo", f.archivo) { viewModel.editar(f.copy(archivo = it, desdeFoto = false)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { Campo("Ancho px", f.ancho, numerico = true) { viewModel.editar(f.copy(ancho = it)) } }
            Column(Modifier.weight(1f)) { Campo("Alto px", f.alto, numerico = true) { viewModel.editar(f.copy(alto = it)) } }
        }
        Campo("Peso (KB)", f.pesoKb, numerico = true) { viewModel.editar(f.copy(pesoKb = it)) }

        Text("Clases anotadas", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClaseAnotada.entries.forEach { clase ->
                FilterChip(
                    selected = clase in f.clases,
                    onClick = { viewModel.alternarClase(clase) },
                    label = { Text("${clase.codigo} · ${clase.etiqueta}") },
                )
            }
        }

        if (estado.erroresEnvio.isNotEmpty()) ListaMensajes(estado.erroresEnvio)

        PrimaryAction(
            text = if (estado.enviando) "Validando..." else "Validar imagen",
            icon = Icons.Default.TaskAlt,
            onClick = viewModel::validar,
            enabled = !estado.enviando,
        )
    }
}

@Composable
private fun TarjetaVeredicto(v: ValidacionImagen) {
    TarjetaSeccion(titulo = "Veredicto del servicio") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(v.archivo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (v.aceptada) InsigniaPositiva("ACEPTADA") else InsigniaNegativa("RECHAZADA")
        }
        FilaDato("Dimensiones", "${v.ancho} × ${v.alto} px")
        FilaDato("Clase para entrenar", v.claseBinaria ?: "sin clase unica")
        if (v.errores.isNotEmpty()) ListaMensajes(v.errores)
        if (v.advertencias.isNotEmpty()) ListaMensajes(v.advertencias, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun TarjetaResumenCalidad(r: ResumenCalidad) {
    TarjetaSeccion(titulo = "Resumen  ·  GET /resumen") {
        FilaDato("Validadas", r.totalValidadas.toString())
        FilaDato("Aceptadas", r.aceptadas.toString())
        FilaDato("Rechazadas", r.rechazadas.toString())
        FilaDato("Tasa de aceptacion", "${r.tasaAceptacionPct} %")
        FilaDato("Con advertencias", r.conAdvertencias.toString())
        if (r.motivosDeRechazo.isNotEmpty()) {
            Text("Motivos de rechazo", style = MaterialTheme.typography.titleSmall)
            r.motivosDeRechazo.forEach { (motivo, n) -> FilaDato(motivo, n.toString()) }
        }
        if (r.aceptadasPorClase.isNotEmpty()) {
            Text("Aceptadas por clase", style = MaterialTheme.typography.titleSmall)
            r.aceptadasPorClase.forEach { (clase, n) -> FilaDato(clase, n.toString()) }
        }
    }
}

@Composable
private fun TarjetaHistorial(
    historial: List<ValidacionImagen>,
    soloRechazadas: Boolean,
    onAlternar: () -> Unit,
) {
    TarjetaSeccion(titulo = if (soloRechazadas) "Rechazadas  ·  GET /validaciones/rechazadas" else "Historial  ·  GET /validaciones") {
        FilterChip(selected = soloRechazadas, onClick = onAlternar, label = { Text("Solo rechazadas") })
        if (historial.isEmpty()) Text("Sin validaciones todavia.")
        historial.take(MAX_HISTORIAL).forEachIndexed { i, v ->
            if (i > 0) HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(v.archivo, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${v.ancho}×${v.alto} · ${v.clases.joinToString().ifEmpty { "sin clases" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (v.aceptada) InsigniaPositiva("OK") else InsigniaNegativa("NO")
            }
        }
        if (historial.size > MAX_HISTORIAL) {
            Text("... y ${historial.size - MAX_HISTORIAL} mas", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val MAX_HISTORIAL = 20
