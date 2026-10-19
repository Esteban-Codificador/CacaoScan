package com.cacaoscan.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.EmptyState
import com.cacaoscan.presentation.common.ErrorState
import com.cacaoscan.presentation.common.LoadingState
import com.cacaoscan.presentation.common.UiState
import com.cacaoscan.presentation.theme.CacaoTheme

/**
 * Historial local de clasificaciones (RF06, RNF05).
 *
 * Los cuatro estados posibles se tratan explicitamente: cargando, vacio, error y
 * lista con datos. El estado vacio no es un error y ofrece la accion util
 * (escanear), en lugar de dejar la pantalla en blanco.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onScanClick: (Long) -> Unit,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingPurge by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    if (confirmingPurge) {
        AlertDialog(
            onDismissRequest = { confirmingPurge = false },
            title = { Text("Borrar todo el historial") },
            text = {
                Text(
                    "Se borraran todas las revisiones y sus fotografias. No se puede deshacer. " +
                        "Para continuar tendra que confirmar su identidad con la huella o el " +
                        "bloqueo del telefono.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingPurge = false
                        viewModel.purgeHistory()
                    },
                ) { Text("Borrar todo") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingPurge = false }) { Text("Cancelar") }
            },
        )
    }

    CacaoScreen(
        title = "Historial",
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            if (state is UiState.Success) {
                IconButton(onClick = { confirmingPurge = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Borrar todo el historial",
                    )
                }
            }
        },
    ) { contentModifier ->
        when (val current = state) {
            is UiState.Loading -> LoadingState(
                message = current.message ?: "Cargando...",
                modifier = contentModifier,
            )

            is UiState.Empty -> EmptyState(
                message = current.message,
                actionText = "Escanear una vaina",
                onAction = onStartScan,
                modifier = contentModifier,
            )

            is UiState.Error -> ErrorState(
                message = current.message,
                modifier = contentModifier,
            )

            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = contentModifier.fillMaxSize(),
            ) {
                items(items = current.data, key = { it.id }) { item ->
                    HistoryRow(item = item, onClick = { onScanClick(item.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: ScanListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = CacaoTheme.semanticColors
    val accent = if (item.isHealthy) semantic.healthy else semantic.affected

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (item.imagePath != null) {
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = if (item.isHealthy) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.WarningAmber
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.conditionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Confianza ${item.confidencePercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.isSimulated) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = semantic.demoWarning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Resultado simulado",
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.demoWarning,
                        )
                    }
                }
            }
        }
    }
}
