package com.cacaoscan.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cacaoscan.presentation.theme.CacaoTheme

/**
 * Componentes compartidos por todas las pantallas.
 *
 * Estan pensados para uso en campo (RNF03): areas de toque grandes, textos
 * legibles y un unico mensaje por pantalla. Ninguno contiene logica de negocio.
 */

/** Altura minima de las acciones principales. Muy por encima de los 48dp minimos. */
private val PrimaryActionHeight = 72.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacaoScreen(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                            )
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

@Composable
fun PrimaryAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PrimaryActionHeight),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PrimaryActionHeight),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Tarjeta de navegacion de la pantalla de inicio: destino, icono y una linea que
 * explica para que sirve. El productor no deberia tener que adivinar.
 */
@Composable
fun NavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    MessageState(
        icon = Icons.Default.ErrorOutline,
        title = "Algo no salio bien",
        message = message,
        tint = MaterialTheme.colorScheme.error,
        actionText = if (onRetry != null) "Reintentar" else null,
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    MessageState(
        icon = Icons.Default.Inbox,
        title = "Todavia no hay nada aqui",
        message = message,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        actionText = actionText,
        onAction = onAction,
        modifier = modifier,
    )
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    message: String,
    tint: Color,
    actionText: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(text = actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Aviso de modo demostracion.
 *
 * Se muestra siempre que el resultado provenga del clasificador simulado y no de
 * un modelo entrenado. No es descartable a proposito: un resultado simulado
 * nunca debe poder confundirse con una clasificacion real.
 */
@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    val semantic = CacaoTheme.semanticColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(semantic.demoWarningContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription =
                    "Aviso: modo demostracion. El resultado es simulado y no proviene de un modelo entrenado."
            },
    ) {
        Icon(
            imageVector = Icons.Default.Science,
            contentDescription = null,
            tint = semantic.demoWarning,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "MODO DEMOSTRACION",
                style = MaterialTheme.typography.labelMedium,
                color = semantic.onDemoWarningContainer,
            )
            Text(
                text = "Resultado simulado. Todavia no hay un modelo entrenado instalado; " +
                    "este valor no tiene validez para decidir sobre el cultivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = semantic.onDemoWarningContainer,
            )
        }
    }
}
