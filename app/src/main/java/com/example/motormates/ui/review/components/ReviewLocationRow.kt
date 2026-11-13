package com.example.motormates.ui.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Lo que se va a guardar como ubicación de la reseña, visible antes de
 * publicar. Existe porque capturar la posición en silencio al tocar
 * "Publicar" dejaba al usuario sin saber qué se adjuntó — y en un emulador,
 * cuya posición por defecto es la sede de Google en California, el marcador
 * aparecía a miles de kilómetros sin ninguna explicación.
 *
 * Tres estados: buscando, resuelta (con ✕ para quitarla) y no disponible
 * (con la opción de reintentar).
 */
@Composable
fun ReviewLocationRow(
    placeName: String?,
    hasLocation: Boolean,
    isLoading: Boolean,
    isUnavailable: Boolean,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reviewLocationRow")
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            // Si no hay ubicación, toda la fila reintenta: es un blanco más
            // grande que un botón pequeño.
            .let { if (isUnavailable) it.clickable(onClick = onRetryClick) else it }
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = if (hasLocation) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.xs))

        Text(
            text = when {
                isLoading -> stringResource(R.string.review_location_loading)
                // Con ubicación pero sin nombre (el geocodificador no
                // respondió) se dice que sí quedó guardada, sin inventar un
                // lugar.
                hasLocation -> placeName ?: stringResource(R.string.review_location_unnamed)
                else -> stringResource(R.string.review_location_unavailable)
            },
            color = if (hasLocation) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("reviewLocationText")
        )

        when {
            hasLocation -> Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.review_location_remove_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onRemoveClick)
                    .testTag("reviewLocationRemove")
            )

            isUnavailable -> Text(
                text = stringResource(R.string.review_location_enable),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
    }
}

@Preview
@Composable
private fun ReviewLocationRowResolvedPreview() {
    MotorMatesTheme {
        ReviewLocationRow(
            placeName = "Chapinero, Bogotá",
            hasLocation = true,
            isLoading = false,
            isUnavailable = false
        )
    }
}

@Preview
@Composable
private fun ReviewLocationRowLoadingPreview() {
    MotorMatesTheme {
        ReviewLocationRow(
            placeName = null,
            hasLocation = false,
            isLoading = true,
            isUnavailable = false
        )
    }
}

@Preview
@Composable
private fun ReviewLocationRowUnavailablePreview() {
    MotorMatesTheme {
        ReviewLocationRow(
            placeName = null,
            hasLocation = false,
            isLoading = false,
            isUnavailable = true
        )
    }
}
