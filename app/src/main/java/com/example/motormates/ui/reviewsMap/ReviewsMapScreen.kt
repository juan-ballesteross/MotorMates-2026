package com.example.motormates.ui.reviewsMap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.R
import com.example.motormates.ui.common.openPointInMapsApp
import com.example.motormates.ui.reviewsMap.components.LocationPermissionDialogs
import com.example.motormates.ui.reviewsMap.components.rememberLocationPermissionHandler

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
    onAuthorClick: (String) -> Unit = {},
    onEventDetailClick: (String) -> Unit = {},
    onCreateEventClick: () -> Unit = {},
    viewModel: ReviewsMapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Se reabre la consulta cada vez que se entra a la pestaña: la ventana de
    // 24 horas se calcula al arrancar, así que volver a entrar es lo que la
    // mantiene fresca.
    LaunchedEffect(Unit) { viewModel.start() }

    // Permiso, ubicación del teléfono y Play Services. El mapa se ve igual
    // sin permiso: esto solo habilita "Mi ubicación".
    val locationHandler = rememberLocationPermissionHandler(
        onAccessChanged = viewModel::onLocationAccessChanged,
        onReady = viewModel::centerOnMyLocation
    )
    LocationPermissionDialogs(locationHandler)

    // Abrir la estación en otra app es cosa de la pantalla y no del ViewModel:
    // necesita el Context de Android, que allá no debe entrar.
    val context = LocalContext.current
    val noMapsAppMessage = stringResource(R.string.station_no_maps_app)

    ReviewsMapContent(
        uiState = uiState,
        onToggleLayer = viewModel::toggleLayer,
        onMarkerClick = viewModel::onMarkerClick,
        onDismissSelection = viewModel::clearSelection,
        onClusterClick = viewModel::onClusterClick,
        onDismissCluster = viewModel::dismissClusterSelection,
        onEventMarkerClick = viewModel::onEventMarkerClick,
        onEventClusterClick = viewModel::onEventClusterClick,
        onDismissEventCluster = viewModel::dismissEventClusterSelection,
        onStationMarkerClick = viewModel::onStationMarkerClick,
        onStationOpenInMaps = { station ->
            val opened = openPointInMapsApp(
                context = context,
                lat = station.lat,
                lng = station.lng,
                label = station.nombre
            )
            // Si no hay ninguna app que atienda geo:, se avisa por el mismo
            // snackbar que usan los fallos de la capa.
            if (!opened) viewModel.showStationsMessage(noMapsAppMessage)
        },
        onStationsMessageShown = viewModel::onStationsMessageShown,
        onEventDetailClick = onEventDetailClick,
        onCreateEventClick = onCreateEventClick,
        onVehicleClick = onVehicleClick,
        onAuthorClick = onAuthorClick,
        onRetryClick = viewModel::retry,
        onCameraFitted = viewModel::onCameraFitted,
        onCameraIdle = viewModel::onCameraIdle,
        onMyLocationClick = locationHandler::request,
        onMyLocationTargetConsumed = viewModel::consumeMyLocationTarget,
        onLocationMessageShown = viewModel::clearLocationMessage,
        modifier = modifier
    )
}
