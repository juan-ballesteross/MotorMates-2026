package com.example.motormates.ui.reviewsMap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.motormates.R
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.ui.reviewsMap.components.ReviewMarkerCard
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.CancellationException

/**
 * El mapa de reseñas recientes, sin estado propio: recibe los marcadores ya
 * listos y avisa de los toques hacia arriba.
 *
 * Nota sobre la convención del proyecto: los demás XScreenContent llevan un
 * @Preview, pero un GoogleMap no se puede renderizar en el previsualizador
 * (necesita Play Services y una superficie de OpenGL). Por eso el @Preview
 * está en ReviewMarkerCard, que es la parte que de verdad se diseña, y el
 * mapa se verifica en el emulador.
 */
@Composable
fun ReviewsMapContent(
    markers: List<ReviewMapMarkerUi>,
    selectedMarker: ReviewMapMarkerUi?,
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    onMarkerClick: (String) -> Unit = {},
    onDismissSelection: () -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
            .testTag("reviewsMapScreen")
    ) {
        ReviewsMapLayer(
            markers = markers,
            onMarkerClick = onMarkerClick,
            onMapClick = onDismissSelection
        )

        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reviewsMapLoading"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            errorMessage != null -> OverlayMessage(
                message = errorMessage,
                tag = "reviewsMapError",
                isError = true,
                onRetryClick = onRetryClick
            )

            // Sin esto, un mapa sin marcadores se lee como si la función
            // estuviera rota en vez de como "todavía no hay nada".
            isEmpty -> OverlayMessage(
                message = stringResource(R.string.reviews_map_empty),
                tag = "reviewsMapEmpty"
            )
        }

        if (selectedMarker != null) {
            ReviewMarkerCard(
                marker = selectedMarker,
                onCardClick = onVehicleClick,
                onCloseClick = onDismissSelection,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.sm)
            )
        }
    }
}

/**
 * El mapa en sí. Va aparte para que ReviewsMapContent se lea de un golpe y
 * para dejar claro qué parte es la que no se puede previsualizar.
 */
@Composable
private fun ReviewsMapLayer(
    markers: List<ReviewMapMarkerUi>,
    modifier: Modifier = Modifier,
    onMarkerClick: (String) -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }

    /**
     * Encuadra la cámara sobre las reseñas que haya, en vez de abrir siempre
     * sobre Bogotá. Sin esto, una reseña publicada fuera de la ciudad (por
     * ejemplo en un emulador, cuya posición por defecto es la sede de Google
     * en California) quedaba fuera de pantalla y parecía que no se había
     * guardado.
     *
     * Se hace una sola vez por lista de marcadores (la llave es el conjunto
     * de ids): el listener reemite con cada like, y reencuadrar en cada
     * emisión le quitaría el mapa de las manos al usuario mientras lo mueve.
     */
    val markerIds = markers.map { it.id }
    LaunchedEffect(markerIds) {
        if (markers.isEmpty()) return@LaunchedEffect

        // Con un solo marcador no se usa newLatLngBounds: el recuadro de un
        // único punto tiene tamaño cero y el mapa se iría al zoom máximo.
        val update = if (markers.size == 1) {
            val only = markers.first()
            CameraUpdateFactory.newLatLngZoom(LatLng(only.lat, only.lng), SINGLE_MARKER_ZOOM)
        } else {
            val bounds = LatLngBounds.builder()
                .apply { markers.forEach { include(LatLng(it.lat, it.lng)) } }
                .build()
            CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX)
        }

        // newLatLngBounds falla si el mapa todavía no tiene tamaño medido
        // (pasa en la primera composición de la pestaña). No es un error que
        // valga la pena mostrar: la cámara se queda donde estaba.
        //
        // La CancellationException se relanza en vez de tragarse: la lanza
        // animate() cuando otro movimiento la interrumpe o cuando se sale de
        // la pestaña, y atraparla rompería la cancelación de la corrutina.
        try {
            cameraPositionState.animate(update)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // El mapa aún no estaba listo para encuadrar.
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // DARK porque la app usa paleta oscura: el mapa claro por defecto
        // desentonaría. Lo resuelve el propio SDK, sin JSON de estilo.
        mapColorScheme = ComposeMapColorScheme.DARK,
        // isMyLocationEnabled se deja en false a propósito: lanza
        // SecurityException si no hay permiso de ubicación, y el punto azul
        // no hace falta para ver dónde reseñó la gente.
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
        // Tocar el mapa cierra la tarjeta abierta.
        onMapClick = { onMapClick() }
    ) {
        markers.forEach { marker ->
            // key(id) es obligatorio: remember se acuerda por posición en la
            // composición, así que sin la llave, al cambiar la lista un
            // MarkerState se reutilizaría con coordenadas de otra reseña y
            // los alfileres saltarían de sitio.
            key(marker.id) {
                Marker(
                    state = rememberUpdatedMarkerState(LatLng(marker.lat, marker.lng)),
                    title = marker.vehicleName,
                    snippet = "${marker.nombreUsuario} · ${marker.tiempoTexto}",
                    onClick = {
                        onMarkerClick(marker.id)
                        // true = "ya lo manejé": si se devuelve false, el SDK
                        // además abre su propia burbuja blanca encima de
                        // nuestra tarjeta.
                        true
                    }
                )
            }
        }
    }
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

/**
 * Bogotá: solo es el respaldo para cuando no hay ningún marcador. Si hay,
 * la cámara se ajusta a ellos.
 */
private val DEFAULT_CENTER = LatLng(4.6097, -74.0817)
private const val DEFAULT_ZOOM = 11f

/** Zoom de barrio, para cuando hay una sola reseña que mostrar. */
private const val SINGLE_MARKER_ZOOM = 14f

/** Margen alrededor del recuadro, para que los pines no queden en el borde. */
private const val BOUNDS_PADDING_PX = 120
