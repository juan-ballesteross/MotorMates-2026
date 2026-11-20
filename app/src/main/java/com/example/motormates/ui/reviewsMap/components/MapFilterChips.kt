package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.R
import com.example.motormates.ui.reviewsMap.MapLayer
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Spacing

/**
 * Los filtros del mapa: qué capas se están mostrando.
 *
 * Va encima del mapa y no flotando dentro, como el resto de los controles,
 * por dos razones: así no choca con el contador, el chip de "N nuevas" ni la
 * tarjeta del marcador abierto, y nunca queda tapado por un snackbar o por
 * una hoja inferior. Al quedar justo debajo de FeedTabsRow se lee como una
 * segunda fila del mismo lenguaje de control, con el mismo FilterChip.
 *
 * Se desplaza en horizontal y no se reparte en varias líneas (FlowRow): con
 * cuatro capas, en un teléfono angosto se partiría en dos líneas y se
 * comería el alto del mapa.
 *
 * @param available las capas que se ofrecen. No son todas las de [MapLayer]:
 *   una capa se ofrece cuando está implementada y cuando se puede consultar
 *   (por ejemplo, la de carga eléctrica necesita su API key configurada).
 */
@Composable
fun MapFilterChips(
    selected: Set<MapLayer>,
    onToggle: (MapLayer) -> Unit,
    modifier: Modifier = Modifier,
    available: List<MapLayer> = MapLayer.entries,
    /** Chip extra "Mis categorías" (onboarding): solo reseñas de esas categorías. */
    showMyCategories: Boolean = false,
    onlyMyCategories: Boolean = false,
    onToggleMyCategories: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter, vertical = Spacing.xs)
            .testTag("reviewsMapFilters"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        available.forEach { layer ->
            FilterChip(
                selected = layer in selected,
                onClick = { onToggle(layer) },
                label = { Text(text = stringResource(layer.labelRes)) },
                modifier = Modifier.testTag("reviewsMapFilter_${layer.name.lowercase()}")
            )
        }
        if (showMyCategories) {
            FilterChip(
                selected = onlyMyCategories,
                onClick = onToggleMyCategories,
                label = { Text(text = "Mis categorías") },
                modifier = Modifier.testTag("reviewsMapFilter_myCategories")
            )
        }
    }
}

private val MapLayer.labelRes: Int
    get() = when (this) {
        MapLayer.REVIEWS -> R.string.reviews_map_filter_reviews
        MapLayer.EVENTS -> R.string.reviews_map_filter_events
        MapLayer.FUEL_STATIONS -> R.string.reviews_map_filter_fuel
        MapLayer.CHARGING_STATIONS -> R.string.reviews_map_filter_charging
    }

@Preview
@Composable
private fun MapFilterChipsPreview() {
    MotorMatesTheme {
        MapFilterChips(
            selected = setOf(MapLayer.REVIEWS, MapLayer.FUEL_STATIONS),
            onToggle = {}
        )
    }
}
