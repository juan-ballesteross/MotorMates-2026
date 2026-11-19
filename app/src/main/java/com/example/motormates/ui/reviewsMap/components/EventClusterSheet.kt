package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.data.model.EventMapMarkerUi
import com.example.motormates.data.model.EventType
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Los eventos de un grupo que el zoom no puede separar porque están en el
 * mismo punto, por ejemplo varios encuentros en el mismo parqueadero. Mismo
 * molde que [ClusterReviewsSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventClusterSheet(
    events: List<EventMapMarkerUi>,
    onEventSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("reviewsMapEventClusterSheet")
    ) {
        Column(modifier = Modifier.padding(bottom = Spacing.lg)) {
            Text(
                text = pluralStringResource(
                    R.plurals.reviews_map_event_cluster_title,
                    events.size,
                    events.size
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )
            events.firstOrNull()?.lugarTexto?.let { place ->
                Text(
                    text = place,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }
            LazyColumn {
                items(events, key = { it.id }) { event ->
                    EventClusterRow(event = event, onClick = { onEventSelected(event.id) })
                }
            }
        }
    }
}

@Composable
private fun EventClusterRow(event: EventMapMarkerUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (event.imageUrl == null) {
                Icon(
                    imageVector = when (event.type) {
                        EventType.CAR_MEET -> Icons.Filled.Groups
                        EventType.PIQUE -> Icons.Filled.EmojiEvents
                    },
                    contentDescription = null
                )
            } else {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${event.nombreOrganizador} · ${event.fechaTexto}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (event.isCancelled) {
            Text(
                text = stringResource(R.string.event_cancelled_label),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}
