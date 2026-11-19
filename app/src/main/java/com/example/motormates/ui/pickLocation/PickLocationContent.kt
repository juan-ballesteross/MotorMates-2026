package com.example.motormates.ui.pickLocation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.ui.reviewsMap.LocationAccess
import com.example.motormates.ui.reviewsMap.components.MapControls
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * El mapa para elegir un punto, sin estado propio.
 *
 * Se elige con una **retícula fija en el centro**: el usuario arrastra el
 * mapa por debajo del pin y confirma. En un teléfono eso es bastante más
 * preciso que tocar un punto exacto con el dedo, y además deja claro en todo
 * momento qué se va a guardar.
 */
@Composable
fun PickLocationContent(
    uiState: PickLocationUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onConfirmClick: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    onMyLocationClick: () -> Unit = {},
    onMyLocationTargetConsumed: () -> Unit = {},
    onLocationMessageShown: () -> Unit = {}
) {
    val cameraPositionState = rememberCameraPositionState {
        position = uiState.initialCamera
            ?.let { CameraPosition.fromLatLngZoom(LatLng(it.lat, it.lng), it.zoom) }
            ?: CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }
    val coroutineScope = rememberCoroutineScope()

    // La ubicación del dispositivo puede llegar después de que el mapa ya se
    // compuso: cuando llega, se mueve la cámara una sola vez.
    LaunchedEffect(uiState.initialCamera) {
        val camera = uiState.initialCamera ?: return@LaunchedEffect
        cameraPositionState.animateSafely(
            CameraUpdateFactory.newLatLngZoom(LatLng(camera.lat, camera.lng), camera.zoom)
        )
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

    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("pickLocationScreen")
    ) {
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                mapColorScheme = if (isDarkPalette) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
                properties = MapProperties(
                    isMyLocationEnabled = uiState.locationAccess == LocationAccess.GRANTED
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false
                )
            )

            // La retícula no es parte del mapa: va encima, fija en el centro.
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = stringResource(R.string.pick_location_crosshair_cd),
                tint = colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(CROSSHAIR_SIZE)
                    .testTag("pickLocationCrosshair")
            )

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.sm)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(colorScheme.surface.copy(alpha = 0.9f))
                    .testTag("pickLocationBack")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pick_location_back_cd),
                    tint = colorScheme.onSurface
                )
            }

            MapControls(
                onZoomIn = {
                    coroutineScope.launch { cameraPositionState.animateSafely(CameraUpdateFactory.zoomIn()) }
                },
                onZoomOut = {
                    coroutineScope.launch { cameraPositionState.animateSafely(CameraUpdateFactory.zoomOut()) }
                },
                onCenter = onMyLocationClick,
                showMyLocation = uiState.locationAccess != LocationAccess.UNAVAILABLE,
                isLocating = uiState.isLocating,
                onMyLocation = onMyLocationClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.md)
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Column(modifier = Modifier.padding(Spacing.gutter)) {
            Text(
                text = stringResource(R.string.pick_location_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val target = cameraPositionState.position.target
                    onConfirmClick(target.latitude, target.longitude)
                },
                enabled = !uiState.isResolvingPlace,
                shape = RoundedCornerShape(Radius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
                    .height(BUTTON_HEIGHT)
                    .testTag("pickLocationConfirm")
            ) {
                if (uiState.isResolvingPlace) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(text = stringResource(R.string.pick_location_confirm))
                }
            }
        }
    }
}

/** Igual que en el mapa de reseñas: una cancelación no es un error. */
private suspend fun com.google.maps.android.compose.CameraPositionState.animateSafely(
    update: com.google.android.gms.maps.CameraUpdate
) {
    try {
        animate(update)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // El mapa aún no está listo: la cámara se queda donde estaba.
    }
}

private val DEFAULT_CENTER = LatLng(4.6097, -74.0817)
private const val DEFAULT_ZOOM = 11f
private val CROSSHAIR_SIZE = 48.dp
private val BUTTON_HEIGHT = 56.dp
