package com.example.motormates.ui.editProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.data.model.GarageVehicleUi
import com.example.motormates.ui.editProfile.components.EditProfileError
import com.example.motormates.ui.editProfile.components.EditProfileDeleteAccountButton
import com.example.motormates.ui.editProfile.components.EditProfileGarageCard
import com.example.motormates.ui.editProfile.components.EditProfileHeader
import com.example.motormates.ui.editProfile.components.EditProfileInfoCard
import com.example.motormates.ui.editProfile.components.EditProfileLogoutButton
import com.example.motormates.ui.editProfile.components.EditProfilePreferencesButton
import com.example.motormates.ui.editProfile.components.EditProfileTopBar
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Spacing

/**
 * Contenido de "Editar perfil". Stateless: recibe todo el estado desde
 * EditProfileScreen y solo reporta cambios vía callbacks.
 *
 * Orden: portada con la foto encima, información personal, mi garaje y al
 * final cerrar sesión. La barra queda fija arriba y lo demás hace scroll.
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
    cars: List<GarageVehicleUi>,
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
    onEditVehicleClick: (vehicleId: String) -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGarageLoading: Boolean = false,
    onEditPreferencesClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditProfileTopBar(onCloseClick = onCloseClick, onSaveClick = onSaveClick, isSaving = isSaving)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                // Edge-to-edge: sin esto el teclado tapa los campos de abajo.
                .imePadding(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.xs,
                bottom = Spacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                EditProfileHeader(
                    coverImageUrl = coverImageUrl,
                    profileImageUrl = profileImageUrl,
                    fullName = fullName,
                    username = username,
                    isCoverUploading = isCoverUploading,
                    isPhotoUploading = isImageUploading,
                    onChangeCoverClick = onChangeCoverClick,
                    onChangePhotoClick = onChangePhotoClick
                )
            }

            // Los errores van juntos arriba, cerca de lo que los causó.
            listOfNotNull(coverUploadError, imageUploadError, saveError).forEach { error ->
                item { EditProfileError(message = error) }
            }

            item {
                EditProfileInfoCard(
                    fullName = fullName,
                    onFullNameChange = onFullNameChange,
                    username = username,
                    onUsernameChange = onUsernameChange,
                    location = location,
                    onLocationChange = onLocationChange,
                    bio = bio,
                    onBioChange = onBioChange
                )
            }

            item {
                EditProfileGarageCard(
                    cars = cars,
                    isLoading = isGarageLoading,
                    onAddVehicleClick = onAddVehicleClick,
                    onEditVehicleClick = onEditVehicleClick
                )
            }

            item { EditProfilePreferencesButton(onClick = onEditPreferencesClick) }
            item { EditProfileLogoutButton(onClick = onLogoutClick) }
            item { EditProfileDeleteAccountButton(onClick = onDeleteAccountClick) }
        }
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun EditProfileScreenContentPreview() {
    MotorMatesTheme {
        EditProfileScreenContent(
            fullName = "Diego Melgarejo",
            onFullNameChange = {},
            username = "diegui",
            onUsernameChange = {},
            location = "Bogotá, Colombia",
            onLocationChange = {},
            bio = "Amante de los roadsters y las rutas de montaña.",
            onBioChange = {},
            cars = listOf(
                GarageVehicleUi("1", "Mazda MX-5", "2020 · Deportivo", null, potencia = "181 hp", aceleracion = "6.5 s"),
                GarageVehicleUi("2", "Renault Duster", "2018 · SUV", null)
            ),
            profileImageUrl = null,
            isImageUploading = false,
            imageUploadError = null,
            coverImageUrl = null,
            isCoverUploading = false,
            coverUploadError = null,
            isSaving = false,
            saveError = null,
            onChangePhotoClick = {},
            onChangeCoverClick = {},
            onCloseClick = {},
            onSaveClick = {},
            onAddVehicleClick = {},
            onEditVehicleClick = {},
            onLogoutClick = {},
            onDeleteAccountClick = {}
        )
    }
}
