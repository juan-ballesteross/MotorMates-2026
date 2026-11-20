package com.example.motormates.ui.aiTuning

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun AiTuningScreen(
    vehicleId: String,
    onBackClick: () -> Unit = {},
    onOpenBuild: (String) -> Unit = {},
    viewModel: AiTuningViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    // En el resultado, "atrás" vuelve al configurador (no se pierde la selección).
    BackHandler(enabled = state.phase == TuningPhase.RESULT) { viewModel.backToConfigurator() }
    BackHandler(enabled = state.phase == TuningPhase.GENERATING) { viewModel.cancelGeneration() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::onOwnPartPicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraPhotoUri?.let(viewModel::onOwnPartPicked) }

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

    AiTuningContent(
        state = state,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = AiTuningActions(
            onBack = onBackClick,
            onRetryLoad = { viewModel.load(vehicleId) },
            onSelectMode = viewModel::selectMode,
            onSelectCategory = viewModel::selectCategory,
            onTogglePart = viewModel::togglePart,
            onApplyPackage = viewModel::applyPackage,
            onClearSelection = viewModel::clearSelection,
            onFreeIdeaChange = viewModel::updateFreeIdea,
            onOwnPartDescriptionChange = viewModel::updateOwnPartDescription,
            onPickOwnPart = { showPhotoSourceDialog = true },
            onRemoveOwnPart = viewModel::removeOwnPart,
            onIdentifyOwnPart = viewModel::identifyOwnPart,
            onGenerate = viewModel::generate,
            onCancelGeneration = viewModel::cancelGeneration,
            onRefineTextChange = viewModel::updateRefineText,
            onRefine = viewModel::refine,
            onRetryInsights = viewModel::loadInsights,
            onLivePrice = viewModel::requestLivePrice,
            onSave = viewModel::save,
            onOpenBuild = onOpenBuild,
            onBackToConfigurator = viewModel::backToConfigurator
        )
    )
}

private fun createPartPhotoUri(context: Context): Uri {
    val photosDir = File(context.cacheDir, "tuning_photos").apply { mkdirs() }
    val file = File(photosDir, "PART_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
