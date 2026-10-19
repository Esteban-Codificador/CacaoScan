package com.cacaoscan.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.PrimaryAction
import com.cacaoscan.presentation.common.SecondaryAction
import com.cacaoscan.security.authz.AuthorizationScope
import com.cacaoscan.security.lock.LockAvailability

/**
 * Demostracion academica de autenticacion y autorizacion locales.
 *
 * Muestra el mecanismo completo y, sobre todo, permite explicarlo: se ve el
 * token real emitido, su cabecera y su payload en claro (porque un JWT va
 * codificado, no cifrado), y se puede comprobar en vivo que un permiso ausente
 * se deniega.
 *
 * Esta pantalla no controla ninguna funcion principal de la aplicacion.
 */
@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    CacaoScreen(
        title = "Seguridad local",
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentModifier ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            InfoBlock(
                title = "Demostracion academica",
                body = "Esta pantalla existe para mostrar como funcionan la autenticacion y la " +
                    "autorizacion locales. Escanear, clasificar y consultar el historial NO " +
                    "dependen de ella.",
            )

            InfoBlock(
                title = "1. Autenticacion",
                body = "La realiza el sistema operativo mediante huella, rostro o la credencial " +
                    "del dispositivo. CacaoScan nunca ve ni guarda su huella ni su PIN: solo " +
                    "recibe si la verificacion fue correcta. No hay cuentas ni servidores.",
            )

            val availabilityText = when (state.availability) {
                LockAvailability.AVAILABLE -> "Disponible en este dispositivo."
                LockAvailability.NOT_ENROLLED ->
                    "El dispositivo no tiene huella ni bloqueo configurado. " +
                        "Configurelo en los ajustes del telefono para probar esta seccion."
                LockAvailability.UNAVAILABLE ->
                    "Este dispositivo no permite autenticacion local."
            }
            Text(
                text = availabilityText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.sessionOpen) {
                SecondaryAction(
                    text = "Cerrar sesion",
                    icon = Icons.Default.LockOpen,
                    onClick = viewModel::closeSession,
                )
            } else {
                PrimaryAction(
                    text = "Autenticarme",
                    icon = Icons.Default.Fingerprint,
                    enabled = state.availability == LockAvailability.AVAILABLE,
                    onClick = viewModel::authenticate,
                )
            }

            InfoBlock(
                title = "2. Autorizacion",
                body = "Tras autenticarse se emite un JSON Web Token firmado con HMAC-SHA256. La " +
                    "clave se genera en este dispositivo y vive en el Android Keystore: no esta " +
                    "en el codigo ni en el APK, y es distinta en cada instalacion.",
            )

            state.decodedToken?.let { token ->
                TokenBlock(label = "Cabecera", content = token.header)
                TokenBlock(label = "Payload", content = token.payload)
                TokenBlock(label = "Firma (recortada)", content = token.signaturePreview)
                Text(
                    text = "Observe que la cabecera y el payload se leen sin ninguna clave: un JWT " +
                        "va codificado en Base64, NO cifrado. Por eso nunca debe llevar datos " +
                        "sensibles. Lo que la firma garantiza es que nadie los ha modificado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            InfoBlock(
                title = "3. Comprobacion de permisos",
                body = "Cada accion protegida exige un permiso concreto. Pruebe a comprobar uno " +
                    "que el token incluye y otro que no.",
            )

            SecondaryAction(
                text = "Comprobar 'history:read'",
                icon = Icons.Default.LockOpen,
                onClick = { viewModel.checkScope(AuthorizationScope.HISTORY_READ) },
            )

            state.lastCheck?.let { check ->
                Text(
                    text = check,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            InfoBlock(
                title = "Que NO resuelve esto",
                body = "Aqui el emisor y el verificador del token son la misma aplicacion, de modo " +
                    "que la firma no aporta confianza frente a terceros: ese es el valor real de " +
                    "JWT y solo aparece entre un servidor y otro servicio. Tampoco hay revocacion, " +
                    "ni proteccion frente a un atacante con acceso fisico y privilegios de root. " +
                    "OTP por SMS, SSO y OAuth 2.0 quedan fuera del alcance porque exigen " +
                    "servidores, y CacaoScan debe funcionar sin conexion.",
            )
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TokenBlock(label: String, content: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .horizontalScroll(rememberScrollState()),
        )
    }
}
