package com.example.motormates.ui.editProfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.GarageCar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de "Editar perfil". El estado (username, bio) vive aquí,
 * sembrado desde UserMocks.sampleUserProfile, igual patrón que PostScreen/
 * RegisterScreen. No hay persistencia real: onSaveClick solo navega hacia
 * atrás, tal como onPublishClick en PostScreen no persiste la publicación.
 */
@Composable
fun EditProfileScreen(
    onCloseClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onAddVehicleClick: () -> Unit = {},
    onEditVehicleClick: (GarageCar) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf(UserMocks.sampleUserProfile.name) }
    var bio by remember { mutableStateOf(UserMocks.sampleUserProfile.bio) }

    EditProfileScreenContent(
        username = username,
        onUsernameChange = { username = it },
        bio = bio,
        onBioChange = { bio = it },
        cars = UserMocks.sampleUserCars,
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
