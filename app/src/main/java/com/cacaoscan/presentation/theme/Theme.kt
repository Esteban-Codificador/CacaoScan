package com.cacaoscan.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CacaoBrown,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0DCCB),
    onPrimaryContainer = CacaoBrownDark,
    secondary = LeafGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFD6),
    onSecondaryContainer = LeafGreenDark,
    tertiary = HuskAmber,
    onTertiary = Color.White,
    background = CacaoCream,
    onBackground = Color(0xFF1F1B18),
    surface = Color.White,
    onSurface = Color(0xFF1F1B18),
    surfaceVariant = Color(0xFFF2E7DD),
    onSurfaceVariant = Color(0xFF52443A),
    outline = Color(0xFF867567),
    error = StateAffected,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = CacaoBrownLight,
    onPrimary = Color(0xFF2A1808),
    primaryContainer = Color(0xFF4A2E19),
    onPrimaryContainer = Color(0xFFF0DCCB),
    secondary = LeafGreenLight,
    onSecondary = Color(0xFF00390A),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFCDEFD6),
    tertiary = HuskAmberLight,
    onTertiary = Color(0xFF3F2A00),
    background = CacaoCreamDark,
    onBackground = Color(0xFFEDE0D9),
    surface = Color(0xFF261D18),
    onSurface = Color(0xFFEDE0D9),
    surfaceVariant = Color(0xFF3B2E27),
    onSurfaceVariant = Color(0xFFD8C6BA),
    outline = Color(0xFFA08D80),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * Colores semanticos que Material 3 no define y que la aplicacion necesita para
 * comunicar el estado sanitario y el modo demostracion. Se exponen por
 * CompositionLocal para que las pantallas no tengan que decidir el color segun
 * el tema claro u oscuro.
 */
@Immutable
data class CacaoSemanticColors(
    val healthy: Color,
    val healthyContainer: Color,
    val onHealthyContainer: Color,
    val affected: Color,
    val affectedContainer: Color,
    val onAffectedContainer: Color,
    val demoWarning: Color,
    val demoWarningContainer: Color,
    val onDemoWarningContainer: Color,
)

private val LightSemanticColors = CacaoSemanticColors(
    healthy = StateHealthy,
    healthyContainer = StateHealthyContainer,
    onHealthyContainer = Color(0xFF0A3D1B),
    affected = StateAffected,
    affectedContainer = StateAffectedContainer,
    onAffectedContainer = Color(0xFF5C1310),
    demoWarning = DemoWarning,
    demoWarningContainer = DemoWarningContainer,
    onDemoWarningContainer = Color(0xFF4A3000),
)

private val DarkSemanticColors = CacaoSemanticColors(
    healthy = Color(0xFF7CD69A),
    healthyContainer = Color(0xFF14512A),
    onHealthyContainer = Color(0xFFCDEFD6),
    affected = Color(0xFFFFB4AB),
    affectedContainer = Color(0xFF7A1F19),
    onAffectedContainer = Color(0xFFFADAD7),
    demoWarning = Color(0xFFFFD07A),
    demoWarningContainer = Color(0xFF5C3D00),
    onDemoWarningContainer = Color(0xFFFFE8B8),
)

private val LocalCacaoSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object CacaoTheme {
    val semanticColors: CacaoSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCacaoSemanticColors.current
}

/**
 * No se usa color dinamico (Material You) de forma intencionada: el significado
 * de "sana" y "afectada" no puede quedar sujeto al fondo de pantalla del usuario.
 */
@Composable
fun CacaoScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalCacaoSemanticColors provides semanticColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CacaoTypography,
            content = content,
        )
    }
}
