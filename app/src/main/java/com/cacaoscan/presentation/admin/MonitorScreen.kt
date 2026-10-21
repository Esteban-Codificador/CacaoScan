package com.cacaoscan.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.SecondaryAction

@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    onNavigateBack: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    CacaoScreen(
        title = "Monitoreo",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = viewModel::alternarSeguimiento) {
                Icon(
                    imageVector = if (estado.siguiendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (estado.siguiendo) "Pausar" else "Reanudar",
                )
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
                    text = if (estado.siguiendo) {
                        "En vivo · GET /metricas cada 3 s · ultima lectura ${estado.ultimaLectura ?: "-"}"
                    } else {
                        "En pausa · ultima lectura ${estado.ultimaLectura ?: "-"}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                estado.lecturas.forEach { TarjetaMetricas(it) }

                SecondaryAction(
                    text = if (estado.generandoTrafico) "Generando..." else "Generar trafico de prueba",
                    icon = Icons.Default.Bolt,
                    onClick = viewModel::generarTrafico,
                    enabled = !estado.generandoTrafico,
                )
                estado.mensaje?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TarjetaMetricas(lectura: LecturaMonitor) {
    val m = lectura.metricas
    TarjetaSeccion(titulo = lectura.servicio.etiqueta) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = m?.let { "v${it.version} · puerto ${lectura.servicio.puerto}" } ?: "puerto ${lectura.servicio.puerto}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            when {
                m == null -> InsigniaNegativa("NO RESPONDE")
                m.degradado -> InsigniaNegativa("DEGRADADO")
                else -> InsigniaPositiva("VIVO")
            }
        }

        if (m == null) {
            lectura.fallo?.let { ListaMensajes(listOf(it)) }
            return@TarjetaSeccion
        }

        FilaDato("Tiempo activo", tiempo(m.tiempoActivoSegundos))
        FilaDato("Peticiones", m.peticionesTotales.toString())
        FilaDato("Errores", "${m.erroresTotales} (${m.tasaErrorPct} %)")
        FilaDato("Latencia mediana", "${m.latenciaMedianaMs} ms")
        FilaDato("Latencia p95", "${m.latenciaP95Ms} ms")
        FilaDato("Latencia maxima", "${m.latenciaMaximaMs} ms")

        if (m.peticionesPorEstado.isNotEmpty()) {
            Text("Por codigo HTTP", style = MaterialTheme.typography.titleSmall)
            m.peticionesPorEstado.toSortedMap().forEach { (codigo, n) -> FilaDato(codigo, n.toString()) }
        }
        if (m.peticionesPorRuta.isNotEmpty()) {
            Text("Rutas mas usadas", style = MaterialTheme.typography.titleSmall)
            m.peticionesPorRuta.entries.sortedByDescending { it.value }.take(5).forEach { (ruta, n) ->
                FilaDato(ruta, n.toString(), monoespaciado = true)
            }
        }
    }
}

private fun tiempo(segundos: Double): String {
    val s = segundos.toLong()
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}
