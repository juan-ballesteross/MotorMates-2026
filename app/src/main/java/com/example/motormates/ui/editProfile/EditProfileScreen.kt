package com.example.motormates.ui.editProfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.data.model.GarageCar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de "Editar perfil". El estado (username, bio, cars) vive
 * en EditProfileViewModel, sembrado desde UserMocks.sampleUserProfile, igual
 * patrón que LoginScreen. No hay persistencia real: onSaveClick solo navega
 * hacia atrás, tal como onPublishClick en PostScreen no persiste la publicación.
 */
@Composable
fun EditProfileScreen(
    onCloseClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onAddVehicleClick: () -> Unit = {},
    onEditVehicleClick: (GarageCar) -> Unit = {},
    viewModel: EditProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val cars by viewModel.cars.collectAsStateWithLifecycle()

    EditProfileScreenContent(
        username = username,
        onUsernameChange = viewModel::updateUsername,
        bio = bio,
        onBioChange = viewModel::updateBio,
        cars = cars,
        onCloseClick = onCloseClick,
        onSaveClick = onSaveClick,
        onAddVehicleClick = onAddVehicleClick,
        onEditVehicleClick = onEditVehicleClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    MotorMatesTheme {
        EditProfileScreen()
    }
}
