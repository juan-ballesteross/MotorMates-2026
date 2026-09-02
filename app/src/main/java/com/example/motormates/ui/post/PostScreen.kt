package com.example.motormates.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.post.components.PostTopBar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de "Nueva publicación". El estado (descripción,
 * vehículo/ubicación etiquetados) vive en PostViewModel y se pasa hacia
 * abajo a los composables stateless PostTopBar y PostScreenContent, igual
 * que VehicleDetailScreen hace con isBookmarked.
 *
 * Como todavía no existen pantallas reales para elegir vehículo o ubicación,
 * tocar esos botones solo alterna un valor de ejemplo (ver PostViewModel);
 * reemplazar por navegación real cuando esas pantallas existan.
 */
@Composable
fun PostScreen(
    onCancelClick: () -> Unit = {},
    onPublishClick: (caption: String) -> Unit = {},
    viewModel: PostViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val caption by viewModel.caption.collectAsStateWithLifecycle()
    val taggedVehicle by viewModel.taggedVehicle.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()

    val canPublish = caption.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PostTopBar(
            canPublish = canPublish,
            onCancelClick = onCancelClick,
            onPublishClick = { onPublishClick(caption) }
        )
        PostScreenContent(
            caption = caption,
            onCaptionChange = viewModel::updateCaption,
            taggedVehicle = taggedVehicle,
            onTagVehicleClick = viewModel::toggleTaggedVehicle,
            location = location,
            onAddLocationClick = viewModel::toggleLocation
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
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.FEED) }
        ) { innerPadding ->
            PostScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
