package com.example.motormates.ui.reviewsMap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * El mapa con las reseñas publicadas en las últimas 24 horas.
 *
 * Hoy se compone dentro de la pestaña "Mapa" del Feed, pero tiene su propio
 * ViewModel y no depende de nada del Feed: si más adelante se quiere como
 * pantalla con ruta propia, basta registrarla en AppNavigation.
 */
@Composable
fun ReviewsMapScreen(
    onVehicleClick: (String) -> Unit = {},
    viewModel: ReviewsMapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Se reabre la consulta cada vez que se entra a la pestaña: la ventana de
    // 24 horas se calcula al arrancar, así que volver a entrar es lo que la
    // mantiene fresca.
    LaunchedEffect(Unit) { viewModel.start() }

    ReviewsMapContent(
        markers = uiState.markers,
        selectedMarker = uiState.selectedMarker,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        isEmpty = uiState.isEmpty,
        onMarkerClick = viewModel::onMarkerClick,
        onDismissSelection = viewModel::clearSelection,
        onVehicleClick = onVehicleClick,
        onRetryClick = viewModel::retry,
        modifier = modifier
    )
}
