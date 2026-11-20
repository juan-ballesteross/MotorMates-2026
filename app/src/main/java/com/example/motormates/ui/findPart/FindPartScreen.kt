package com.example.motormates.ui.findPart

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun FindPartScreen(
    onBack: () -> Unit,
    onTuneWithPart: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FindPartViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // Con la pantalla a la vista no hace falta notificar el resultado.
    LifecycleStartEffect(Unit) {
        viewModel.setScreenVisible(true)
        onStopOrDispose { viewModel.setScreenVisible(false) }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }
    LaunchedEffect(state.openTuning) {
        if (state.openTuning) {
            viewModel.onTuningOpened()
            onTuneWithPart()
        }
    }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onPhotoPicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraPhotoUri?.let(viewModel::onPhotoPicked) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createPartPhotoUri(context)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Foto de la pieza") },
            text = { Text("¿De dónde quieres sacar la foto?") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Galería") }
            }
        )
    }

    FindPartContent(
        state = state,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = FindPartActions(
            onBack = onBack,
            onPickPhoto = { showPhotoSourceDialog = true },
            onRemovePhoto = viewModel::removePhoto,
            onDescriptionChange = viewModel::updateDescription,
            onSearch = viewModel::search,
            onNewSearch = viewModel::newSearch,
            onTuneWithPart = viewModel::tuneWithPart,
            onFinish = viewModel::finish
        )
    )
}

private fun createPartPhotoUri(context: Context): Uri {
    val photosDir = File(context.cacheDir, "tuning_photos").apply { mkdirs() }
    val file = File(photosDir, "FIND_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
