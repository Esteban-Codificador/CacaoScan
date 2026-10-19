package com.cacaoscan.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.presentation.common.PrimaryAction

/**
 * Acceso administrativo, mostrado al abrir la aplicacion.
 *
 * En el primer arranque pide crear la contrasena; despues, introducirla. No hay
 * registro de usuarios ni recuperacion por correo: no existe servidor, y la
 * unica forma de restablecer la contrasena es desinstalar la aplicacion, lo que
 * borra tambien el historial. Eso se advierte en pantalla para que nadie lo
 * descubra por las malas.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is LoginUiState.Authenticated) onAuthenticated()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Text(
                text = "CacaoScan",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Acceso administrativo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))

            when (val current = state) {
                LoginUiState.Loading, LoginUiState.Authenticated ->
                    CircularProgressIndicator(modifier = Modifier.height(56.dp))

                is LoginUiState.Setup -> SetupForm(
                    state = current,
                    onCreate = viewModel::onCreatePassword,
                )

                is LoginUiState.Authenticate -> AuthenticateForm(
                    state = current,
                    onSubmit = viewModel::onSubmitPassword,
                )
            }
        }
    }
}

@Composable
private fun SetupForm(
    state: LoginUiState.Setup,
    onCreate: (String, String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    Text(
        text = "Es la primera vez que se abre la aplicacion. Cree una contrasena " +
            "para proteger el acceso.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))

    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = "Contrasena nueva",
        imeAction = ImeAction.Next,
    )
    Spacer(Modifier.height(14.dp))
    PasswordField(
        value = confirmation,
        onValueChange = { confirmation = it },
        label = "Repita la contrasena",
        imeAction = ImeAction.Done,
        onDone = { onCreate(password, confirmation) },
    )

    ErrorText(state.error)
    Spacer(Modifier.height(24.dp))

    PrimaryAction(
        text = if (state.busy) "Guardando..." else "Crear contrasena",
        icon = Icons.Default.Login,
        enabled = !state.busy,
        onClick = { onCreate(password, confirmation) },
    )

    Spacer(Modifier.height(20.dp))
    Text(
        text = "Guardela bien: no hay forma de recuperarla. Restablecerla exige " +
            "desinstalar la aplicacion, lo que borra tambien el historial.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AuthenticateForm(
    state: LoginUiState.Authenticate,
    onSubmit: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = "Contrasena",
        enabled = !state.isLocked && !state.busy,
        imeAction = ImeAction.Done,
        onDone = { onSubmit(password) },
    )

    if (state.isLocked) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Acceso bloqueado. Espere ${state.lockedSeconds} segundos.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    } else {
        ErrorText(state.error)
    }

    Spacer(Modifier.height(24.dp))

    PrimaryAction(
        text = if (state.busy) "Verificando..." else "Entrar",
        icon = Icons.Default.Login,
        enabled = !state.busy && !state.isLocked,
        onClick = { onSubmit(password) },
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onDone() },
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (visible) {
                        "Ocultar contrasena"
                    } else {
                        "Mostrar contrasena"
                    },
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    )
}

@Composable
private fun ErrorText(message: String?) {
    if (message == null) return
    Spacer(Modifier.height(12.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
