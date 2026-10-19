package com.cacaoscan.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.cacaoscan.presentation.common.DemoModeBanner
import com.cacaoscan.presentation.common.ErrorState
import com.cacaoscan.presentation.common.LoadingState
import com.cacaoscan.presentation.common.SecondaryAction
import com.cacaoscan.presentation.common.UiState
import com.cacaoscan.presentation.theme.CacaoTheme

/**
 * Detalle de una revision guardada (RF06).
 */
@Composable
fun HistoryDetailScreen(
    viewModel: HistoryDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onNavigateBack()
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Eliminar esta revision") },
            text = { Text("Se borrara del historial junto con su fotografia. No se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        viewModel.onDelete()
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") }
            },
        )
    }

    CacaoScreen(
        title = "Revision",
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    ) { contentModifier ->
        when (val current = state) {
            is UiState.Loading -> LoadingState(
                message = current.message ?: "Cargando...",
                modifier = contentModifier,
            )

            is UiState.Empty -> ErrorState(
                message = current.message,
                modifier = contentModifier,
            )

            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = onNavigateBack,
                modifier = contentModifier,
            )

            is UiState.Success -> DetailContent(
                detail = current.data,
                onDeleteClick = { confirmingDelete = true },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: ScanDetail,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = CacaoTheme.semanticColors
    val accent = if (detail.isHealthy) semantic.healthy else semantic.affected
    val container =
        if (detail.isHealthy) semantic.healthyContainer else semantic.affectedContainer
    val onContainer =
        if (detail.isHealthy) semantic.onHealthyContainer else semantic.onAffectedContainer

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (detail.isSimulated) {
            DemoModeBanner()
        }

        if (detail.imagePath != null) {
            AsyncImage(
                model = detail.imagePath,
                contentDescription = "Fotografia de la vaina revisada",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
            )
        } else {
            Text(
                text = "Esta revision no tiene fotografia guardada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(container)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (detail.isHealthy) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.WarningAmber
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = detail.conditionLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = onContainer,
                    )
                    Text(
                        text = detail.dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainer,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Confianza del modelo: ${detail.confidencePercent}%",
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(18.dp),
        ) {
            Text(
                text = "Detalle tecnico",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Tiempo de analisis: ${detail.inferenceTimeMs} ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Modelo: ${detail.modelVersion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SecondaryAction(
            text = "Eliminar revision",
            icon = Icons.Default.Delete,
            onClick = onDeleteClick,
        )
    }
}
