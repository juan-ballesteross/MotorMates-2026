package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.data.model.StationKind
import com.example.motormates.data.model.StationMapMarkerUi
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * La tarjeta de la estación seleccionada en el mapa.
 *
 * Es la más simple de las tres tarjetas del mapa, y a propósito: de una
 * gasolinera solo se sabe el nombre y la dirección — el field mask barato no
 * trae más, ver PlacesStationDataSource — así que no hay detalle que abrir y la
 * tarjeta no es pulsable. La única acción es llevarse el punto a otra app.
 *
 * No hay pantalla de detalle de estación por lo mismo: no habría nada que
 * mostrar en ella que no esté ya acá.
 */
@Composable
fun StationMarkerCard(
    station: StationMapMarkerUi,
    modifier: Modifier = Modifier,
    onOpenInMapsClick: (StationMapMarkerUi) -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.sm)
            .testTag("stationMarkerCard")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (station.kind) {
                        StationKind.FUEL -> Icons.Filled.LocalGasStation
                        StationKind.CHARGING -> Icons.Filled.EvStation
                    },
                    contentDescription = null, // el nombre de al lado ya lo dice
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // La dirección puede faltar: Places no la garantiza para todos
                // los lugares, y una línea vacía se ve peor que ninguna línea.
                station.direccion?.let { direccion ->
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = direccion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.testTag("stationCardClose")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.review_close_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onOpenInMapsClick(station) },
                modifier = Modifier.testTag("stationOpenInMaps")
            ) {
                Icon(
                    imageVector = Icons.Filled.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(text = stringResource(R.string.station_open_in_maps))
            }
        }
    }
}

@Preview
@Composable
private fun StationMarkerCardPreview() {
    MotorMatesTheme {
        StationMarkerCard(
            station = StationMapMarkerUi(
                id = "abc",
                kind = StationKind.FUEL,
                lat = 4.6766,
                lng = -74.0482,
                nombre = "Terpel Calle 85",
                direccion = "Calle 85 #15-20, Bogotá"
            )
        )
    }
}

@Preview
@Composable
private fun StationMarkerCardNoAddressPreview() {
    MotorMatesTheme {
        StationMarkerCard(
            station = StationMapMarkerUi(
                id = "def",
                kind = StationKind.FUEL,
                lat = 4.6766,
                lng = -74.0482,
                nombre = "Gasolinera"
            )
        )
    }
}
