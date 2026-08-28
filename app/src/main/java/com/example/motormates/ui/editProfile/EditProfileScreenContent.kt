package com.example.motormates.ui.editProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.GarageCar
import com.example.motormates.ui.editProfile.components.EditProfileAddVehicleButton
import com.example.motormates.ui.editProfile.components.EditProfileBioField
import com.example.motormates.ui.editProfile.components.EditProfileGarageItem
import com.example.motormates.ui.editProfile.components.EditProfilePhotoSection
import com.example.motormates.ui.editProfile.components.EditProfileTopBar
import com.example.motormates.ui.register.components.RegisterTextField
import com.example.motormates.ui.theme.MotorMatesBackground

/**
 * Contenido scrollable de "Editar perfil". Stateless: recibe todo el estado
 * desde EditProfileScreen y solo reporta cambios vía callbacks, igual que
 * UserScreenContent/PostScreenContent.
 */
@Composable
fun EditProfileScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    cars: List<GarageCar>,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (GarageCar) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            EditProfileTopBar(onCloseClick = onCloseClick, onSaveClick = onSaveClick)
        }
        item {
            EditProfilePhotoSection()
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
            EditProfileBioField(value = bio, onValueChange = onBioChange)
        }
        item {
            Text(
                text = stringResource(R.string.edit_profile_garage_title),
                color = Color.White,
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
    }
}
