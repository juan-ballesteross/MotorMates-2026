package com.example.motormates.ui.review

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.R
import com.example.motormates.ui.review.components.NewReviewTopBar
import com.example.motormates.ui.review.components.PhotoSourceDialog
import java.io.File

/**
 * Sirve para crear y para editar: reviewId == null significa crear.
 * El auto reseñado ya no llega por parámetro — lo resuelve el ViewModel a
 * partir del id, en vez de que el grafo de navegación lo busque.
 */
@Composable
fun NewReviewScreen(
    vehicleId: String,
    reviewId: String? = null,
    onCloseClick: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: NewReviewViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing = reviewId != null
    val context = LocalContext.current
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::addPhoto) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraPhotoUri?.let(viewModel::addPhoto) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraPhotoUri(context)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Se intenta capturar con lo que el usuario haya respondido: si negó el
    // permiso, el data source devuelve null y la fila queda en "sin
    // ubicación", con la opción de reintentar.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.captureLocation()
    }

    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun requestLocation() {
        if (hasLocationPermission()) {
            viewModel.captureLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    /**
     * La ubicación se resuelve al ENTRAR, no al publicar: así el usuario ve
     * qué se va a adjuntar antes de comprometerse, y el botón de publicar no
     * abre diálogos (que era lo que antes permitía publicar dos veces de dos
     * toques seguidos).
     *
     * Solo al crear: editar una reseña no cambia dónde se publicó.
     */
    LaunchedEffect(isEditing) {
        if (!isEditing) requestLocation()
    }

    LaunchedEffect(vehicleId, reviewId) {
        viewModel.start(vehicleId, reviewId)
    }

    // LaunchedEffect para la navegación: la pantalla no decide cuándo
    // cerrarse, reacciona a que el estado diga que ya se guardó.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            onDismiss = { showPhotoSourceDialog = false },
            onCameraClick = {
                showPhotoSourceDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGalleryClick = {
                showPhotoSourceDialog = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NewReviewTopBar(
            onCloseClick = onCloseClick,
            title = if (isEditing) stringResource(R.string.review_edit_top_bar_title) else null
        )

        val car = uiState.car
        when {
            car == null && uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            car == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "No se pudo cargar el vehículo",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> NewReviewScreenContent(
                car = car,
                rating = uiState.rating,
                onRatingChange = viewModel::updateRating,
                experience = uiState.experience,
                onExperienceChange = viewModel::updateExperience,
                selectedAspects = uiState.selectedAspects,
                onToggleAspect = viewModel::toggleAspect,
                existingImageUrls = uiState.existingImageUrls,
                newPhotoUris = uiState.newPhotoUris,
                onAddPhotoClick = { showPhotoSourceDialog = true },
                onRemoveExistingPhoto = viewModel::removeExistingPhoto,
                onRemoveNewPhoto = viewModel::removeNewPhoto,
                canPublish = uiState.rating > 0 && !uiState.isSaving,
                onPublishClick = { viewModel.save(vehicleId) },
                isEditing = isEditing,
                errorMessage = uiState.errorMessage,
                locationPlaceName = uiState.capturedLocation?.placeName,
                hasLocation = uiState.capturedLocation != null,
                isLocationLoading = uiState.isLocationLoading,
                isLocationUnavailable = uiState.isLocationUnavailable,
                onRetryLocationClick = ::requestLocation,
                onRemoveLocationClick = viewModel::removeLocation
            )
        }
    }
}

private fun createCameraPhotoUri(context: Context): Uri {
    val photosDir = File(context.cacheDir, "review_photos").apply { mkdirs() }
    val file = File(photosDir, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
