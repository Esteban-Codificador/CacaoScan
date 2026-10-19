package com.cacaoscan.presentation.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cacaoscan.presentation.common.CacaoScreen
import com.cacaoscan.presentation.common.LoadingState
import com.cacaoscan.presentation.common.PrimaryAction
import com.cacaoscan.presentation.common.SecondaryAction

/**
 * Captura o seleccion de la imagen a analizar (RF01, RF02).
 *
 * La pantalla no accede a la camara ni decodifica imagenes: solo emite eventos
 * al ViewModel. El permiso de camara se solicita en el momento en que se
 * necesita y nunca al abrir la aplicacion; ademas, si el usuario lo rechaza,
 * la seleccion desde galeria sigue disponible y el flujo no queda bloqueado.
 */
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onResultReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraController by remember { mutableStateOf<CameraController?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.onImagePicked(uri) }

    LaunchedEffect(state.step) {
        if (state.step == ScanStep.RESULT) onResultReady()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    CacaoScreen(
        title = "Escanear vaina",
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentModifier ->
        when (state.step) {
            ScanStep.CLASSIFYING -> LoadingState(
                message = "Analizando la vaina...",
                modifier = contentModifier,
            )

            ScanStep.PREVIEW, ScanStep.RESULT -> PreviewContent(
                state = state,
                onAnalyze = viewModel::onAnalyze,
                onRetake = viewModel::onRetake,
                modifier = contentModifier,
            )

            ScanStep.CAPTURE -> CaptureContent(
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onControllerReady = { cameraController = it },
                onTakePhoto = {
                    cameraController?.takePhoto(
                        onCaptured = viewModel::onPhotoCaptured,
                        onError = { /* se refleja en el snackbar del ViewModel */ },
                    )
                },
                onPickFromGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun CaptureContent(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onControllerReady: (CameraController) -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    onControllerReady = onControllerReady,
                    onError = { },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "Para tomar la foto de la vaina, la aplicacion necesita permiso " +
                        "para usar la camara.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(28.dp),
                )
            }
        }

        Text(
            text = "Encuadre una sola vaina, con buena luz y sin sombras fuertes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (hasCameraPermission) {
            PrimaryAction(
                text = "Tomar foto",
                icon = Icons.Default.CameraAlt,
                onClick = onTakePhoto,
            )
        } else {
            PrimaryAction(
                text = "Permitir camara",
                icon = Icons.Default.CameraAlt,
                onClick = onRequestPermission,
            )
        }

        SecondaryAction(
            text = "Elegir de la galeria",
            icon = Icons.Default.PhotoLibrary,
            onClick = onPickFromGallery,
        )
    }
}

@Composable
private fun PreviewContent(
    state: ScanUiState,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val bitmap = state.image
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Fotografia de la vaina que se va a analizar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }

        Text(
            text = "Revise que la vaina se vea completa y enfocada antes de analizar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(0.dp))

        PrimaryAction(
            text = "Analizar",
            icon = Icons.Default.Science,
            onClick = onAnalyze,
            enabled = state.canAnalyze,
        )

        SecondaryAction(
            text = "Tomar otra foto",
            icon = Icons.Default.Refresh,
            onClick = onRetake,
        )
    }
}
