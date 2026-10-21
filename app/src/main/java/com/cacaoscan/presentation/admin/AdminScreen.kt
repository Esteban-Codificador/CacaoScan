package com.cacaoscan.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.domain.model.admin.EstadoServicio
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.NavigationCard
import com.cacaoscan.presentation.common.SecondaryAction

/**
 * Administracion: los dos microservicios del proyecto, vistos desde la app.
 */
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    onModelosClick: () -> Unit,
    onCalidadClick: () -> Unit,
    onMonitorClick: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    CacaoScreen(
        title = "Administracion",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = viewModel::comprobar) {
                Icon(Icons.Default.Refresh, contentDescription = "Comprobar servicios")
            }
        },
    ) { contentModifier ->
        val motivo = estado.denegado
        if (motivo != null) {
            AccesoDenegado(motivo = motivo, onIniciarSesion = onIniciarSesion, modifier = contentModifier)
            return@CacaoScreen
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text("Microservicios", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Dos servicios independientes que corren en el equipo del proyecto. " +
                    "La aplicacion los consume por la red local. El escaneo de vainas no usa " +
                    "esta conexion: sigue funcionando sin internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TarjetaSeccion(
                titulo = "Conexion",
                subtitulo = "Emulador: 10.0.2.2  ·  Telefono real: la IP del equipo que muestra operar.py",
            ) {
                OutlinedTextField(
                    value = estado.anfitrionEditado,
                    onValueChange = viewModel::editarAnfitrion,
                    label = { Text("Direccion del equipo") },
                    singleLine = true,
                    isError = estado.errorAnfitrion != null,
                    supportingText = estado.errorAnfitrion?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.guardarAnfitrion() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryAction(
                    text = "Guardar y comprobar",
                    icon = Icons.Default.Save,
                    onClick = viewModel::guardarAnfitrion,
                )
            }

            TarjetaSeccion(titulo = "Estado de los servicios") {
                if (estado.comprobando && estado.estados.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Consultando /salud...")
                    }
                }
                estado.estados.forEach { FilaEstadoServicio(it, estado.anfitrion) }
            }

            NavigationCard(
                title = "Registro de Modelos",
                description = "Microservicio 1 · versiones del modelo y cual es apto para publicar",
                icon = Icons.Default.Inventory2,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onModelosClick,
            )
            NavigationCard(
                title = "Control de Calidad",
                description = "Microservicio 2 · decide si una foto sirve para entrenar",
                icon = Icons.Default.VerifiedUser,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onCalidadClick,
            )
            NavigationCard(
                title = "Monitoreo",
                description = "Peticiones, errores y latencia de ambos servicios en vivo",
                icon = Icons.Default.Insights,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onMonitorClick,
            )
        }
    }
}

@Composable
private fun FilaEstadoServicio(estado: EstadoServicio, anfitrion: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(estado.servicio.etiqueta, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "$anfitrion:${estado.servicio.puerto}" +
                        (estado.version?.let { "  ·  v$it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (estado.vivo) InsigniaPositiva("VIVO") else InsigniaNegativa("CAIDO")
        }
        estado.motivo?.let {
            Text(it.mensaje, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
