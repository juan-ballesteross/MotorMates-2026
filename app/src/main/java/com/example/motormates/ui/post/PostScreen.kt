package com.example.motormates.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.post.components.PostTopBar
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de "Nueva publicación". Aquí vive todo el estado local
 * (descripción, vehículo/ubicación etiquetados) y se pasa hacia abajo a los
 * composables stateless PostTopBar y PostScreenContent, igual que
 * CarDetailScreen hace con isBookmarked.
 *
 * Como todavía no existen pantallas reales para elegir vehículo o ubicación,
 * tocar esos botones solo alterna un valor de ejemplo (mismo patrón que
 * onBookmarkClick en CarDetailScreen); reemplazar por navegación real cuando
 * esas pantallas existan.
 */
@Composable
fun PostScreen(
    onCancelClick: () -> Unit = {},
    onPublishClick: (caption: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var caption by remember { mutableStateOf("") }
    var taggedVehicle by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf<String?>(null) }

    val canPublish = caption.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
    ) {
        PostTopBar(
            canPublish = canPublish,
            onCancelClick = onCancelClick,
            onPublishClick = { onPublishClick(caption) }
        )
        PostScreenContent(
            caption = caption,
            onCaptionChange = { caption = it },
            taggedVehicle = taggedVehicle,
            // TODO: reemplazar por navegación a una pantalla real de selección de vehículo.
            onTagVehicleClick = {
                taggedVehicle = if (taggedVehicle == null) "Porsche 911 GT3" else null
            },
            location = location,
            // TODO: reemplazar por navegación/GPS real para elegir ubicación.
            onAddLocationClick = {
                location = if (location == null) "Bogotá, Colombia" else null
            }
        )
    }
}

/**
 * A diferencia de PostScreen, este preview sí agrega un Scaffold con
 * MainBottomNavBar solo para visualizar cómo se ve la pantalla completa
 * (tal como la muestra el mockup). En la app real ese bottomBar ya lo pone
 * el Scaffold único de MainActivity.kt, así que no se duplica al navegar.
 */
@Preview(showBackground = true)
@Composable
private fun PostScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MotorMatesBackground,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.FEED) }
        ) { innerPadding ->
            PostScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
