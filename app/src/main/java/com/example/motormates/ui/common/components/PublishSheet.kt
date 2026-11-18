package com.example.motormates.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Spacing

/**
 * Qué se va a publicar: una publicación o un evento.
 *
 * Aparece al tocar el "+" de la barra inferior, que antes iba directo a la
 * pantalla de publicación. Es un toque más, a cambio de que crear un evento
 * se pueda descubrir desde cualquier pestaña y no solo desde el mapa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishSheet(
    onPostClick: () -> Unit,
    onEventClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("publishSheet")
    ) {
        Column(modifier = Modifier.padding(bottom = Spacing.lg)) {
            Text(
                text = stringResource(R.string.publish_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )
            PublishOption(
                icon = Icons.Filled.PhotoCamera,
                label = stringResource(R.string.publish_sheet_post),
                description = stringResource(R.string.publish_sheet_post_description),
                testTag = "publishSheetPost",
                onClick = onPostClick
            )
            PublishOption(
                icon = Icons.Filled.Event,
                label = stringResource(R.string.publish_sheet_event),
                description = stringResource(R.string.publish_sheet_event_description),
                testTag = "publishSheetEvent",
                onClick = onEventClick
            )
        }
    }
}

@Composable
private fun PublishOption(
    icon: ImageVector,
    label: String,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // la etiqueta de al lado ya lo dice
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun PublishSheetPreview() {
    MotorMatesTheme {
        // ModalBottomSheet no se previsualiza; se deja la fila, que es lo
        // que de verdad se diseña.
        Column {
            PublishOption(
                icon = Icons.Filled.Event,
                label = "Evento",
                description = "Un Car Meet o un pique",
                testTag = "preview",
                onClick = {}
            )
        }
    }
}
