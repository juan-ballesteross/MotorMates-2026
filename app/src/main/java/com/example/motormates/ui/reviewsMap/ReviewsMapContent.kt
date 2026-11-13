package com.example.motormates.ui.reviewsMap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.ui.reviewsMap.components.ClusterReviewsSheet
import com.example.motormates.ui.reviewsMap.components.MapControls
import com.example.motormates.ui.reviewsMap.components.MarkerPalette
import com.example.motormates.ui.reviewsMap.components.ReviewClusterBubble
import com.example.motormates.ui.reviewsMap.components.ReviewMarkerCard
import com.example.motormates.ui.reviewsMap.components.ReviewPhotoMarker
import com.example.motormates.ui.reviewsMap.components.rememberMarkerBitmaps
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * El mapa de reseñas recientes, sin estado propio: recibe el estado ya
 * listo y avisa de los toques hacia arriba.
 *
 * Nota sobre la convención del proyecto: los demás XScreenContent llevan un
 * @Preview, pero un GoogleMap no se puede renderizar en el previsualizador
 * (necesita Play Services y una superficie de OpenGL). Por eso los @Preview
 * están en ReviewMarkerCard y ReviewPhotoMarker, que son las partes que de
 * verdad se diseñan, y el mapa se verifica en el emulador.
 */
@Composable
fun ReviewsMapContent(
    uiState: ReviewsMapUiState,
    modifier: Modifier = Modifier,
    onMarkerClick: (String) -> Unit = {},
    onDismissSelection: () -> Unit = {},
    onClusterClick: (List<String>) -> Boolean = { false },
    onDismissCluster: () -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onRetryClick: () -> Unit = {},
    onCameraFitted: () -> Unit = {},
    onCameraIdle: (MapCamera) -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onMyLocationTargetConsumed: () -> Unit = {},
    onLocationMessageShown: () -> Unit = {}
) {
    val markers = uiState.markers
    val cameraPositionState = rememberCameraPositionState {
        // Al volver a la pestaña el mapa se recrea; se restaura donde quedó.
        position = uiState.savedCamera
            ?.let { CameraPosition.fromLatLngZoom(LatLng(it.lat, it.lng), it.zoom) }
            ?: CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }
    val coroutineScope = rememberCoroutineScope()
    val boundsPaddingPx = with(LocalDensity.current) { BOUNDS_PADDING.roundToPx() }

    fun fitAllMarkers() {
        coroutineScope.launch {
            cameraPositionState.animateSafely(markers.fitUpdate(boundsPaddingPx))
            onCameraFitted()
        }
    }

    // Solo la primera vez que hay marcadores. Antes se reencuadraba con cada
    // emisión del listener: un like o una reseña nueva hacía saltar la
    // cámara mientras el usuario estaba mirando otra zona.
    LaunchedEffect(markers.isNotEmpty(), uiState.hasFittedInitially) {
        if (markers.isNotEmpty() && !uiState.hasFittedInitially) fitAllMarkers()
    }

    // Se guarda la cámara cada vez que se detiene, para restaurarla al volver.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            onCameraIdle(MapCamera(target.latitude, target.longitude, cameraPositionState.position.zoom))
        }
    }

    LaunchedEffect(uiState.myLocationTarget) {
        val target = uiState.myLocationTarget ?: return@LaunchedEffect
        cameraPositionState.animateSafely(
            CameraUpdateFactory.newLatLngZoom(LatLng(target.lat, target.lng), target.zoom)
        )
        onMyLocationTargetConsumed()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.locationMessage) {
        val message = uiState.locationMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onLocationMessageShown()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reviewsMapScreen")
    ) {
        ReviewsMapLayer(
            markers = markers,
            selectedMarkerId = uiState.selectedMarkerId,
            showMyLocation = uiState.locationAccess == LocationAccess.GRANTED,
            cameraPositionState = cameraPositionState,
            onMarkerClick = onMarkerClick,
            onClusterClick = { ids, positions ->
                // Si el ViewModel no abrió la lista, el grupo se puede separar acercando.
                if (!onClusterClick(ids)) {
                    coroutineScope.launch { cameraPositionState.animateSafely(positions.fitUpdate(boundsPaddingPx)) }
                }
            },
            onMapClick = onDismissSelection
        )

        if (uiState.isLoading) {
            // Barra fina y no un spinner encima: con red lenta se sigue viendo el mapa.
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .testTag("reviewsMapLoading")
            )
        }

        if (markers.isNotEmpty()) {
            // Deja claro el filtro que pidió el cliente.
            MapLabel(
                text = pluralStringResource(R.plurals.reviews_map_count, markers.size, markers.size),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.sm)
                    .testTag("reviewsMapCount")
            )
        }

        if (uiState.newMarkersCount > 0) {
            AssistChip(
                onClick = ::fitAllMarkers,
                label = {
                    Text(
                        pluralStringResource(R.plurals.reviews_map_new_chip, uiState.newMarkersCount, uiState.newMarkersCount)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.xl)
                    .testTag("reviewsMapNewChip")
            )
        }

        MapControls(
            onZoomIn = { coroutineScope.launch { cameraPositionState.animateSafely(CameraUpdateFactory.zoomIn()) } },
            onZoomOut = { coroutineScope.launch { cameraPositionState.animateSafely(CameraUpdateFactory.zoomOut()) } },
            onCenter = ::fitAllMarkers,
            showMyLocation = uiState.locationAccess != LocationAccess.UNAVAILABLE,
            isLocating = uiState.isLocating,
            onMyLocation = onMyLocationClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Spacing.md)
        )

        when {
            uiState.errorMessage != null -> OverlayMessage(
                message = uiState.errorMessage,
                tag = "reviewsMapError",
                isError = true,
                onRetryClick = onRetryClick
            )

            // Sin esto, un mapa sin marcadores se lee como si la función
            // estuviera rota en vez de como "todavía no hay nada".
            uiState.isEmpty -> OverlayMessage(
                message = stringResource(R.string.reviews_map_empty),
                tag = "reviewsMapEmpty"
            )
        }

        uiState.selectedMarker?.let { selected ->
            ReviewMarkerCard(
                marker = selected,
                onCardClick = onVehicleClick,
                onAuthorClick = onAuthorClick,
                onCloseClick = onDismissSelection,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.sm)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    val clusterSelection = uiState.clusterSelection
    if (clusterSelection.isNotEmpty()) {
        ClusterReviewsSheet(
            markers = clusterSelection,
            onMarkerSelected = onMarkerClick,
            onDismiss = onDismissCluster
        )
    }
}

/**
 * El mapa en sí. Va aparte para que ReviewsMapContent se lea de un golpe y
 * para dejar claro qué parte es la que no se puede previsualizar.
 */
@Composable
private fun ReviewsMapLayer(
    markers: List<ReviewMapMarkerUi>,
    selectedMarkerId: String?,
    showMyLocation: Boolean,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    onMarkerClick: (String) -> Unit = {},
    onClusterClick: (ids: List<String>, positions: List<LatLng>) -> Unit = { _, _ -> },
    onMapClick: () -> Unit = {}
) {
    val markerSizePx = with(LocalDensity.current) { MARKER_PHOTO_SIZE.roundToPx() }
    val photoUrls = remember(markers) { markers.mapNotNull { it.vehicleImageUrl }.toSet() }
    val photos = rememberMarkerBitmaps(urls = photoUrls, sizePx = markerSizePx)

    // El mapa sigue la paleta que la app está mostrando, no el modo del
    // sistema: en esta app están invertidos (ver MotorMatesTheme), así que
    // isSystemInDarkTheme() daría el mapa contrario al resto de la pantalla.
    // Se decide por el fondo del tema activo, que es lo que de verdad se ve.
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.background.luminance() < 0.5f
    // Se arma aquí y no dentro del marcador: el contenido de Clustering se
    // rasteriza aparte y no conviene que dependa de MaterialTheme.
    val palette = remember(colorScheme) { MarkerPalette.from(colorScheme) }

    // Cambiar la selección, que llegue una foto o que cambie la paleta cambia
    // el ítem (es data class), y eso es lo que hace que Clustering lo redibuje.
    val items = remember(markers, selectedMarkerId, photos.keys, isDarkPalette) {
        markers.map { marker ->
            ReviewClusterItem(
                marker = marker,
                isSelected = marker.id == selectedMarkerId,
                hasPhoto = marker.vehicleImageUrl in photos,
                isDarkMap = isDarkPalette
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // Lo resuelve el propio SDK, sin JSON de estilo.
        mapColorScheme = if (isDarkPalette) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        // Solo con permiso: isMyLocationEnabled lanza SecurityException sin
        // él. La pantalla vuelve a revisar el permiso en cada ON_RESUME.
        properties = MapProperties(isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            // El botón propio de MapControls reemplaza al del SDK.
            myLocationButtonEnabled = false
        ),
        // Tocar el mapa cierra la tarjeta abierta.
        onMapClick = { onMapClick() }
    ) {
        Clustering(
            items = items,
            onClusterClick = { cluster ->
                onClusterClick(cluster.items.map { it.marker.id }, cluster.items.map { it.position })
                // true = "ya lo manejé": evita que la librería haga su propio zoom.
                true
            },
            onClusterItemClick = { item ->
                onMarkerClick(item.marker.id)
                // true: si no, el SDK además abre su burbuja blanca encima de la tarjeta.
                true
            },
            clusterContent = { cluster ->
                ReviewClusterBubble(
                    count = cluster.size,
                    photos = cluster.items.take(3).map { item -> item.marker.vehicleImageUrl?.let(photos::get) },
                    palette = palette
                )
            },
            clusterItemContent = { item ->
                ReviewPhotoMarker(
                    photo = item.marker.vehicleImageUrl?.let(photos::get),
                    rating = item.marker.calificacion,
                    isSelected = item.isSelected,
                    palette = palette
                )
            }
        )
    }
}

@Composable
private fun MapLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
    )
}

@Composable
private fun OverlayMessage(
    message: String,
    tag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onRetryClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.gutter)
                .clip(RoundedCornerShape(Radius.lg))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.md)
                .testTag(tag),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (onRetryClick != null) {
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .padding(top = Spacing.sm)
                        .testTag("reviewsMapRetry")
                ) {
                    Text(text = stringResource(R.string.reviews_map_retry))
                }
            }
        }
    }
}

/** Encuadre para una lista de marcadores: Bogotá si no hay, zoom de barrio si hay uno. */
private fun List<ReviewMapMarkerUi>.fitUpdate(paddingPx: Int): CameraUpdate =
    map { LatLng(it.lat, it.lng) }.fitUpdate(paddingPx)

@JvmName("fitUpdateLatLng")
private fun List<LatLng>.fitUpdate(paddingPx: Int): CameraUpdate = when (size) {
    0 -> CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    1 -> CameraUpdateFactory.newLatLngZoom(first(), SINGLE_MARKER_ZOOM)
    else -> CameraUpdateFactory.newLatLngBounds(
        LatLngBounds.builder().apply { forEach(::include) }.build(),
        paddingPx
    )
}

/**
 * animate falla si el mapa todavía no terminó de cargar (newLatLngBounds
 * necesita conocer el tamaño de la vista). En ese caso no se mueve la
 * cámara y ya: no es un error que deba ver el usuario.
 */
private suspend fun CameraPositionState.animateSafely(update: CameraUpdate) {
    try {
        animate(update)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // El mapa aún no estaba listo.
    }
}

/**
 * Bogotá: solo es el respaldo para cuando no hay ningún marcador. Si hay,
 * la cámara se ajusta a ellos.
 */
private val DEFAULT_CENTER = LatLng(4.6097, -74.0817)
private const val DEFAULT_ZOOM = 11f

/** Zoom de barrio, para cuando hay una sola reseña que mostrar. */
private const val SINGLE_MARKER_ZOOM = 14f

/** Margen alrededor del recuadro, para que los marcadores no queden en el borde. */
private val BOUNDS_PADDING = 64.dp

/** Tamaño al que se descargan las fotos: el del marcador seleccionado, con margen. */
private val MARKER_PHOTO_SIZE = 64.dp
