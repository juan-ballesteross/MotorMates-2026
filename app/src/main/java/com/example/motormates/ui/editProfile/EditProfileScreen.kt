package com.example.motormates.ui.editProfile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditProfileScreen(
    onCloseClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onAddVehicleClick: () -> Unit = {},
    onEditVehicleClick: (vehicleId: String) -> Unit = {},
    onLoggedOut: () -> Unit = {},
    viewModel: EditProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Al volver de editar o publicar un carro, el garaje se recarga.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadGarage() }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::uploadProfileImage)
    }
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::uploadCoverImage)
    }

    EditProfileScreenContent(
        fullName = uiState.fullName,
        onFullNameChange = viewModel::updateFullName,
        username = uiState.username,
        onUsernameChange = viewModel::updateUsername,
        location = uiState.location,
        onLocationChange = viewModel::updateLocation,
        bio = uiState.bio,
        onBioChange = viewModel::updateBio,
        cars = uiState.cars,
        profileImageUrl = uiState.profileImageUrl,
        isImageUploading = uiState.isImageUploading,
        imageUploadError = uiState.imageUploadError,
        coverImageUrl = uiState.coverImageUrl,
        isCoverUploading = uiState.isCoverUploading,
        coverUploadError = uiState.coverUploadError,
        isSaving = uiState.isSaving,
        saveError = uiState.saveError,
        onChangePhotoClick = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onChangeCoverClick = {
            coverPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onCloseClick = onCloseClick,
        onSaveClick = { viewModel.saveProfile(onSaved = onSaveClick) },
        onAddVehicleClick = onAddVehicleClick,
        onEditVehicleClick = onEditVehicleClick,
        onLogoutClick = { viewModel.logOut(onLoggedOut) },
        modifier = modifier,
        isGarageLoading = uiState.isGarageLoading
    )
}
