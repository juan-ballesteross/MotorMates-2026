package com.example.motormates.ui.editProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.GarageCar
import com.example.motormates.ui.editProfile.components.EditProfileAddVehicleButton
import com.example.motormates.ui.editProfile.components.EditProfileBioField
import com.example.motormates.ui.editProfile.components.EditProfileGarageItem
import com.example.motormates.ui.editProfile.components.EditProfileLogoutButton
import com.example.motormates.ui.editProfile.components.EditProfileCoverSection
import com.example.motormates.ui.editProfile.components.EditProfilePhotoSection
import com.example.motormates.ui.editProfile.components.EditProfileTopBar
import com.example.motormates.ui.register.components.RegisterTextField
import com.example.motormates.ui.theme.Spacing

/**
 * Contenido scrollable de "Editar perfil". Stateless: recibe todo el estado
 * desde EditProfileScreen y solo reporta cambios vía callbacks, igual que
 * UserScreenContent/PostScreenContent.
 */
@Composable
fun EditProfileScreenContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    cars: List<GarageCar>,
    profileImageUrl: String?,
    isImageUploading: Boolean,
    imageUploadError: String?,
    coverImageUrl: String?,
    isCoverUploading: Boolean,
    coverUploadError: String?,
    isSaving: Boolean,
    saveError: String?,
    onChangePhotoClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (GarageCar) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // La barra queda fija arriba y solo el formulario hace scroll.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditProfileTopBar(onCloseClick = onCloseClick, onSaveClick = onSaveClick)
        EditProfileForm(
            fullName = fullName,
            onFullNameChange = onFullNameChange,
            username = username,
            onUsernameChange = onUsernameChange,
            location = location,
            onLocationChange = onLocationChange,
            bio = bio,
            onBioChange = onBioChange,
            cars = cars,
            profileImageUrl = profileImageUrl,
            isImageUploading = isImageUploading,
            imageUploadError = imageUploadError,
            coverImageUrl = coverImageUrl,
            isCoverUploading = isCoverUploading,
            coverUploadError = coverUploadError,
            saveError = saveError,
            onChangePhotoClick = onChangePhotoClick,
            onChangeCoverClick = onChangeCoverClick,
            onAddVehicleClick = onAddVehicleClick,
            onEditVehicleClick = onEditVehicleClick,
            onLogoutClick = onLogoutClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditProfileForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    cars: List<GarageCar>,
    profileImageUrl: String?,
    isImageUploading: Boolean,
    imageUploadError: String?,
    coverImageUrl: String?,
    isCoverUploading: Boolean,
    coverUploadError: String?,
    saveError: String?,
    onChangePhotoClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (GarageCar) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = Spacing.xs,
            bottom = Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            EditProfileCoverSection(
                coverImageUrl = coverImageUrl,
                isUploading = isCoverUploading,
                onChangeCoverClick = onChangeCoverClick
            )
        }
        coverUploadError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
        item {
            EditProfilePhotoSection(
                profileImageUrl = profileImageUrl,
                isUploading = isImageUploading,
                onChangePhotoClick = onChangePhotoClick
            )
        }
        imageUploadError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
        saveError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
        item {
            RegisterTextField(
                label = stringResource(R.string.edit_profile_fullname_label),
                value = fullName,
                onValueChange = onFullNameChange,
                placeholder = stringResource(R.string.edit_profile_fullname_placeholder)
            )
        }
        item {
            RegisterTextField(
                label = stringResource(R.string.edit_profile_username_label),
                value = username,
                onValueChange = onUsernameChange,
                placeholder = stringResource(R.string.edit_profile_username_placeholder)
            )
        }
        item {
            RegisterTextField(
                label = stringResource(R.string.edit_profile_location_label),
                value = location,
                onValueChange = onLocationChange,
                placeholder = stringResource(R.string.edit_profile_location_placeholder)
            )
        }
        item {
            EditProfileBioField(value = bio, onValueChange = onBioChange)
        }
        item {
            Text(
                text = stringResource(R.string.edit_profile_garage_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(cars) { car ->
            EditProfileGarageItem(car = car, onEditClick = { onEditVehicleClick(car) })
        }
        item {
            EditProfileAddVehicleButton(onClick = onAddVehicleClick)
        }
        item {
            EditProfileLogoutButton(onClick = onLogoutClick)
        }
    }
}
