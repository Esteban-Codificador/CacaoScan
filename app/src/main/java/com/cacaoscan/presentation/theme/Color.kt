package com.cacaoscan.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de CacaoScan.
 *
 * Se eligen tonos de cacao y hoja en lugar de la paleta morada por defecto de
 * Material 3 para que la aplicacion se lea como una herramienta agricola y no
 * como una aplicacion generica. Los colores de estado (sana / afectada) se
 * mantienen deliberadamente separados de la marca: deben ser inequivocos.
 */

// Marca
val CacaoBrown = Color(0xFF5D3A21)
val CacaoBrownLight = Color(0xFF8A6142)
val CacaoBrownDark = Color(0xFF32200F)
val CacaoCream = Color(0xFFFFF8F2)
val CacaoCreamDark = Color(0xFF1C1512)

val LeafGreen = Color(0xFF2E7D32)
val LeafGreenLight = Color(0xFF60AD5E)
val LeafGreenDark = Color(0xFF005005)

val HuskAmber = Color(0xFFB26A00)
val HuskAmberLight = Color(0xFFE59B3C)

// Estado sanitario. No se reutilizan los colores de marca a proposito:
// el resultado debe distinguirse de un vistazo, incluso a plena luz del sol.
val StateHealthy = Color(0xFF1B7F3B)
val StateHealthyContainer = Color(0xFFCDEFD6)
val StateAffected = Color(0xFFB3261E)
val StateAffectedContainer = Color(0xFFFADAD7)

// Aviso de modo demostracion (sin modelo entrenado).
val DemoWarning = Color(0xFF8A5A00)
val DemoWarningContainer = Color(0xFFFFE8B8)
