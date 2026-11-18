package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.StationKind
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * El marcador de una gasolinera: un círculo pequeño con el ícono del surtidor.
 *
 * Sin punta abajo, al contrario que [EventMarker] y [ReviewPhotoMarker], y más
 * chico que los dos. Es deliberado: las estaciones son telón de fondo — se
 * miran de reojo mientras se busca otra cosa — y con punta y tamaño igual
 * competirían con las capas que el usuario vino a ver. Son también muchas más:
 * hasta veinte en pantalla.
 *
 * La paleta llega por parámetro y no se lee de MaterialTheme acá dentro: el
 * contenido de Clustering se rasteriza fuera de la composición.
 */
@Composable
fun StationMarker(
    kind: StationKind,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    palette: MarkerPalette = MarkerPalette.Default
) {
    val size = if (isSelected) SELECTED_SIZE else NORMAL_SIZE
    val borderColor = if (isSelected) palette.selectedBorder else palette.border

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.fillFor(kind))
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kind.icon,
            contentDescription = null, // lo describe el título del marcador
            tint = Color.White,
            modifier = Modifier.size(size / 2)
        )
    }
}

/**
 * La burbuja de un grupo de estaciones.
 *
 * Siempre lleva ícono, al contrario que [EventClusterBubble]: ahí el grupo
 * podía mezclar Car Meets con piques y mostrar uno solo habría sido engañoso,
 * mientras que acá cada capa consulta un único tipo, así que un grupo nunca
 * mezcla gasolineras con cargadores.
 */
@Composable
fun StationClusterBubble(
    count: Int,
    kind: StationKind,
    modifier: Modifier = Modifier,
    palette: MarkerPalette = MarkerPalette.Default
) {
    Box(
        modifier = modifier
            .size(CLUSTER_SIZE)
            .clip(CircleShape)
            .background(palette.fillFor(kind))
            .border(2.dp, palette.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kind.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(CLUSTER_SIZE / 2)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            Text(
                // Igual que las otras burbujas: por encima de 99 el número no
                // cabe sin encoger la fuente.
                text = if (count > MAX_COUNT) "$MAX_COUNT+" else count.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

private val StationKind.icon
    get() = when (this) {
        StationKind.FUEL -> Icons.Filled.LocalGasStation
        StationKind.CHARGING -> Icons.Filled.EvStation
    }

/**
 * El color distingue las dos capas cuando están encendidas a la vez: a 30 dp,
 * el surtidor y el enchufe se parecen demasiado para fiarse solo del ícono.
 */
private fun MarkerPalette.fillFor(kind: StationKind) = when (kind) {
    StationKind.FUEL -> fuelStationFill
    StationKind.CHARGING -> chargingStationFill
}

private val NORMAL_SIZE: Dp = 30.dp
private val SELECTED_SIZE: Dp = 38.dp
private val CLUSTER_SIZE: Dp = 34.dp
private const val MAX_COUNT = 99

@Preview
@Composable
private fun StationMarkerPreview() {
    MotorMatesTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            StationMarker(kind = StationKind.FUEL, isSelected = false)
        }
    }
}

@Preview
@Composable
private fun StationMarkerSelectedPreview() {
    MotorMatesTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            StationMarker(kind = StationKind.CHARGING, isSelected = true)
        }
    }
}

@Preview
@Composable
private fun StationClusterBubblePreview() {
    MotorMatesTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            StationClusterBubble(count = 12, kind = StationKind.FUEL)
        }
    }
}
