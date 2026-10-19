package com.cacaoscan.presentation.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.domain.model.ClassificationOutput
import com.cacaoscan.domain.model.PodCondition
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.DemoModeBanner
import com.cacaoscan.presentation.common.ErrorState
import com.cacaoscan.presentation.common.PrimaryAction
import com.cacaoscan.presentation.common.SecondaryAction
import com.cacaoscan.presentation.scan.ScanViewModel
import com.cacaoscan.presentation.theme.CacaoTheme

/**
 * Resultado de la clasificacion con su nivel de confianza (RF04).
 *
 * Criterio de presentacion: el porcentaje se rotula siempre como "confianza del
 * modelo" y nunca como probabilidad de acierto ni como diagnostico. Ademas se
 * acompana de una linea que recuerda que el resultado es una orientacion y no
 * sustituye la revision del productor. Presentarlo como una verdad absoluta
 * seria enganoso, especialmente con un modelo cuya exactitud aun no ha sido
 * evaluada (RNF04).
 */
@Composable
fun ResultScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val result = state.result

    CacaoScreen(
        title = "Resultado",
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    ) { contentModifier ->
        if (result == null) {
            ErrorState(
                message = "No hay un resultado para mostrar.",
                onRetry = onNavigateBack,
                modifier = contentModifier,
            )
            return@CacaoScreen
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            if (result.isSimulated) {
                DemoModeBanner()
            }

            state.image?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Vaina analizada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp)),
                )
            }

            ConditionCard(result = result)

            Text(
                text = "Este resultado es una orientacion basada en la fotografia. " +
                    "Ante cualquier duda, revise la vaina directamente en el cultivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TechnicalDetails(result = result)

            Spacer(Modifier.height(4.dp))

            if (state.isSaved) {
                PrimaryAction(
                    text = "Listo",
                    icon = Icons.Default.Done,
                    onClick = onDone,
                )
                Text(
                    text = "La revision quedo guardada en el historial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PrimaryAction(
                    text = if (state.isSaving) "Guardando..." else "Guardar en el historial",
                    icon = Icons.Default.Save,
                    onClick = viewModel::onSave,
                    enabled = !state.isSaving,
                )
                SecondaryAction(
                    text = "Salir sin guardar",
                    icon = Icons.Default.Done,
                    onClick = onDone,
                )
            }
        }
    }
}

@Composable
private fun ConditionCard(result: ClassificationOutput, modifier: Modifier = Modifier) {
    val semantic = CacaoTheme.semanticColors
    val isHealthy = result.condition == PodCondition.HEALTHY

    val container: Color = if (isHealthy) semantic.healthyContainer else semantic.affectedContainer
    val onContainer: Color =
        if (isHealthy) semantic.onHealthyContainer else semantic.onAffectedContainer
    val accent: Color = if (isHealthy) semantic.healthy else semantic.affected

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Estado de la vaina",
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer,
                )
                Text(
                    text = if (isHealthy) "Se ve sana" else "Senales de monilia",
                    style = MaterialTheme.typography.headlineMedium,
                    color = onContainer,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Confianza del modelo: ${result.confidence.percent}%",
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { result.confidence.fraction },
            color = accent,
            trackColor = accent.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Indica que tan seguro esta el modelo de esta categoria; " +
                "no es la probabilidad de que el diagnostico sea correcto.",
            style = MaterialTheme.typography.bodyMedium,
            color = onContainer,
        )
    }
}

/**
 * Detalle tecnico visible para el usuario avanzado y para la sustentacion:
 * incluye el tiempo real de inferencia, que es la evidencia de RNF01.
 */
@Composable
private fun TechnicalDetails(result: ClassificationOutput, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Detalle tecnico",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            result.probabilities.forEach { (condition, confidence) ->
                val label = when (condition) {
                    PodCondition.HEALTHY -> "Sana"
                    PodCondition.MONILIA_AFFECTED -> "Afectada por monilia"
                }
                Text(
                    text = "$label: ${confidence.percent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Tiempo de analisis: ${result.inferenceTimeMs} ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Modelo: ${result.modelVersion ?: "sin identificar"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
