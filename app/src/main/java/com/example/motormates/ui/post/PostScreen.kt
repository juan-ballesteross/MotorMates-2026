package com.example.motormates.ui.post

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.post.components.PostTopBar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Publicar un vehículo propio, o editarlo si llega vehicleId. Al terminar
 * avisa con el id (onPublished) para que la navegación decida a dónde ir;
 * al eliminarlo avisa con onDeleted.
 */
@Composable
fun PostScreen(
    onCancelClick: () -> Unit = {},
    onPublished: (vehicleId: String) -> Unit = {},
    viewModel: PostViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    vehicleId: String? = null,
    onDeleted: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vehicleId) {
        vehicleId?.let(viewModel::startEditing)
    }

    LaunchedEffect(uiState.wasDeleted) {
        if (uiState.wasDeleted) {
            viewModel.onDeletedHandled()
            onDeleted()
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Eliminar vehículo") },
            text = {
                Text(
                    "Se quitará de tu garaje y ya no se podrá comparar. Las reseñas que " +
                        "tenga seguirán en los perfiles de quienes las escribieron. Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") }
            }
        )
    }

    // Selector de fotos del sistema: no necesita permiso de almacenamiento.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = viewModel::onImagePicked
    )

    LaunchedEffect(uiState.publishedVehicleId) {
        uiState.publishedVehicleId?.let { vehicleId ->
            viewModel.onPublishedHandled()
            onPublished(vehicleId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PostTopBar(
            canPublish = uiState.canPublish,
            onCancelClick = onCancelClick,
            onPublishClick = viewModel::publish,
            title = if (uiState.isEditing) "Editar vehículo" else stringResource(R.string.post_top_bar_title),
            actionLabel = if (uiState.isEditing) "Guardar" else stringResource(R.string.post_publish_button)
        )

        val loadError = uiState.loadError
        when {
            uiState.isLoadingVehicle -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            loadError != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = loadError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                TextButton(onClick = viewModel::retryLoad) { Text("Reintentar") }
            }

            else -> PostForm(uiState = uiState, viewModel = viewModel, onPickPhoto = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            })
        }
    }
}

@Composable
private fun PostForm(uiState: PostUiState, viewModel: PostViewModel, onPickPhoto: () -> Unit) {
    PostScreenContent(
        form = uiState.form,
        photo = uiState.photoModel,
        errors = uiState.errors,
        isPublishing = uiState.isPublishing,
        canPublish = uiState.canPublish,
        errorMessage = uiState.errorMessage,
        onPickPhotoClick = onPickPhoto,
        onRemovePhotoClick = viewModel::removeImage,
        onBrandChange = viewModel::onBrandChange,
        onModelChange = viewModel::onModelChange,
        onYearChange = viewModel::onYearChange,
        onCategorySelect = viewModel::onCategorySelect,
        onPotenciaChange = viewModel::onPotenciaChange,
        onAceleracionChange = viewModel::onAceleracionChange,
        onVelocidadChange = viewModel::onVelocidadChange,
        onTraccionSelect = viewModel::onTraccionSelect,
        onPublishClick = viewModel::publish,
        isEditing = uiState.isEditing,
        isDeleting = uiState.isDeleting,
        onDeleteClick = viewModel::askDelete
    )
}

@Preview(showBackground = true)
@Composable
private fun PostScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.FEED) }
        ) { innerPadding ->
            PostScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
