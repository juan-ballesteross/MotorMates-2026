package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Controles propios del mapa (los del SDK se apagan porque no siguen la
 * paleta de la app).
 *
 * "Ver todas" y "Mi ubicación" son botones distintos: antes el de encuadrar
 * usaba el ícono de ubicación, y se leía como "llévame a donde estoy".
 */
@Composable
fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier,
    showMyLocation: Boolean = false,
    isLocating: Boolean = false,
    onMyLocation: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showMyLocation) {
            RoundControl(onClick = onMyLocation, tag = "reviewsMapMyLocation") {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.reviews_map_my_location_cd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
        }

        RoundControl(onClick = onCenter, tag = "reviewsMapFitAll") {
            Icon(
                imageVector = Icons.Default.ZoomOutMap,
                contentDescription = stringResource(R.string.reviews_map_fit_all_cd),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Controles de zoom (+ / -) agrupados
        Column(
            modifier = Modifier
                .width(48.dp)
                .shadow(4.dp, RoundedCornerShape(Radius.lg))
                .clip(RoundedCornerShape(Radius.lg))
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Acercar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(
                modifier = Modifier.width(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Alejar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RoundControl(onClick: () -> Unit, tag: String, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, RoundedCornerShape(Radius.lg))
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .testTag(tag)
    ) {
        content()
    }
}
