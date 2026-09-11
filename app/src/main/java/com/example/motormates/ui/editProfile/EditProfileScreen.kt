package com.example.motormates.ui.editProfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.data.model.GarageCar
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun EditProfileScreen(
    onCloseClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onAddVehicleClick: () -> Unit = {},
    onEditVehicleClick: (GarageCar) -> Unit = {},
    onLoggedOut: () -> Unit = {},
    viewModel: EditProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditProfileScreenContent(
        username = uiState.username,
        onUsernameChange = viewModel::updateUsername,
        bio = uiState.bio,
        onBioChange = viewModel::updateBio,
        cars = uiState.cars,
        onCloseClick = onCloseClick,
        onSaveClick = onSaveClick,
        onAddVehicleClick = onAddVehicleClick,
        onEditVehicleClick = onEditVehicleClick,
        onLogoutClick = { viewModel.logOut(onLoggedOut) },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    MotorMatesTheme {
        EditProfileScreenContent(
            username = "Rodrigo",
            onUsernameChange = {},
            bio = "Amante de los autos clásicos",
            onBioChange = {},
            cars = emptyList(),
            onCloseClick = {},
            onSaveClick = {},
            onAddVehicleClick = {},
            onEditVehicleClick = {},
            onLogoutClick = {}
        )
    }
}
