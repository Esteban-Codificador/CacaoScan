package com.cacaoscan.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.NavigationCard
import com.cacaoscan.presentation.theme.CacaoScanTheme

/**
 * Pantalla de inicio.
 *
 * Tres destinos y nada mas. La accion principal (escanear) ocupa la primera
 * posicion y el mayor peso visual, de modo que el flujo mas frecuente se alcanza
 * con un solo toque desde la apertura de la aplicacion (RNF03).
 */
@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSecurityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CacaoScreen(
        title = "CacaoScan",
        modifier = modifier,
        actions = {
            // Acceso a la demostracion academica de seguridad. Se deja fuera de las
            // tarjetas principales para no anadir pasos al flujo del productor.
            IconButton(onClick = onSecurityClick) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = "Seguridad local")
            }
        },
    ) { contentModifier ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Revise el estado de sus vainas",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Tome una foto de la vaina y la aplicacion le indicara si se ve sana " +
                    "o con senales de monilia. Funciona sin internet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            NavigationCard(
                title = "Escanear vaina",
                description = "Tomar una foto o elegir una de la galeria",
                icon = Icons.Default.CameraAlt,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onScanClick,
            )

            NavigationCard(
                title = "Historial",
                description = "Revisiones que ha hecho antes",
                icon = Icons.Default.History,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onHistoryClick,
            )

            NavigationCard(
                title = "Informacion",
                description = "Que es la monilia y como usar la aplicacion",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onInfoClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    CacaoScanTheme {
        HomeScreen(
            onScanClick = {},
            onHistoryClick = {},
            onInfoClick = {},
            onSecurityClick = {},
        )
    }
}
