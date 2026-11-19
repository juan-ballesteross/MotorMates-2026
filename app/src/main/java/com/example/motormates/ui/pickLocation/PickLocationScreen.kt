package com.example.motormates.ui.pickLocation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.ui.reviewsMap.components.LocationPermissionDialogs
import com.example.motormates.ui.reviewsMap.components.rememberLocationPermissionHandler

/**
 * Elegir en el mapa dónde será un evento.
 *
 * Es una pantalla completa y no un mini-mapa dentro del formulario: el propio
 * proyecto ya documenta (ver FeedScreen) que un GoogleMap no puede ir dentro
 * de un contenedor con scroll, y además un mapa de alto fijo dentro de un
 * formulario desplazable le roba el gesto de arrastre al usuario.
 *
 * Que sea otra ruta no pierde lo escrito en el formulario: el ViewModel de
 * NewEvent está atado a su entrada del back stack, que sigue en la pila
 * mientras esta pantalla está encima.
 */
@Composable
fun PickLocationScreen(
    initialLat: Double?,
    initialLng: Double?,
    onBackClick: () -> Unit,
    onPicked: (ReviewLocation) -> Unit,
    viewModel: PickLocationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialLat, initialLng) { viewModel.start(initialLat, initialLng) }

    // Mismo manejo de permiso que el mapa de reseñas: sin él la pantalla
    // funciona igual, solo que sin punto azul ni "Mi ubicación".
    val locationHandler = rememberLocationPermissionHandler(
        onAccessChanged = viewModel::onLocationAccessChanged,
        onReady = viewModel::centerOnMyLocation
    )
    LocationPermissionDialogs(locationHandler)

    PickLocationContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onConfirmClick = { lat, lng -> viewModel.confirm(lat, lng, onPicked) },
        onMyLocationClick = locationHandler::request,
        onMyLocationTargetConsumed = viewModel::consumeMyLocationTarget,
        onLocationMessageShown = viewModel::clearLocationMessage,
        modifier = modifier
    )
}
