package com.example.motormates.ui.editProfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.data.model.GarageCar
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun EditProfileScreen(
    onCloseClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onAddVehicleClick: () -> Unit = {},
    onEditVehicleClick: (GarageCar) -> Unit = {},
    viewModel: EditProfileViewModel = viewModel(),
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
