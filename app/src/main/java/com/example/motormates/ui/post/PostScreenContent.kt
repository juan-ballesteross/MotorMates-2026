package com.example.motormates.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.ui.post.components.PostActionButton
import com.example.motormates.ui.post.components.PostCaptionField
import com.example.motormates.ui.post.components.PostGalleryPreview
import com.example.motormates.ui.theme.MotorMatesBackground

/**
 * Todo el estado viene del padre (state hoisting) — igual que en
 * CarDetailScreenContent, este composable no guarda nada con remember,
 * solo pinta lo que le llega desde PostScreen.
 */
@Composable
fun PostScreenContent(
    caption: String,
    onCaptionChange: (String) -> Unit,
    taggedVehicle: String?,
    onTagVehicleClick: () -> Unit,
    location: String?,
    onAddLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
            .verticalScroll(rememberScrollState())
    ) {
        PostGalleryPreview()

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(20.dp))

            PostCaptionField(value = caption, onValueChange = onCaptionChange)
            Spacer(modifier = Modifier.height(16.dp))

            PostActionButton(
                icon = Icons.Filled.DirectionsCar,
                label = stringResource(R.string.post_tag_vehicle_button),
                selectedValue = taggedVehicle,
                onClick = onTagVehicleClick
            )
            Spacer(modifier = Modifier.height(12.dp))

            PostActionButton(
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.post_add_location_button),
                selectedValue = location,
                onClick = onAddLocationClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
