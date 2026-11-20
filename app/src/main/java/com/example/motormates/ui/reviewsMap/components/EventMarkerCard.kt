package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventMapMarkerUi
import com.example.motormates.data.model.EventType
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * La tarjeta del evento seleccionado en el mapa.
 *
 * Es una tarjeta propia y no la burbuja del SDK de Maps, por la misma razón
 * que [ReviewMarkerCard]: la burbuja no deja poner botones ni controlar el
 * estilo.
 *
 * Solo recibe un [EventMapMarkerUi]: todo lo que muestra (si ya pasó, si es
 * mío, los textos de fecha) viene resuelto desde el mapper.
 */
@Composable
fun EventMarkerCard(
    event: EventMapMarkerUi,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {},
    onOrganizerClick: (String) -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onCardClick(event.id) }
            .padding(Spacing.sm)
            .testTag("eventMarkerCard")
    ) {
        Row(verticalAlignment = Alignment.Top) {
            EventThumbnail(event)

            Spacer(modifier = Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                EventTypeChip(type = event.type, isCancelled = event.isCancelled)

                Spacer(modifier = Modifier.height(Spacing.xxs))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                IconLine(
                    icon = Icons.Filled.Schedule,
                    text = listOfNotNull(event.fechaTexto, event.horarioTexto).joinToString(" · ")
                )

                event.lugarTexto?.let { IconLine(icon = Icons.Filled.LocationOn, text = it) }
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.testTag("eventMarkerCardClose")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.event_close_card_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        event.descripcion?.let { descripcion ->
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrganizerRow(event = event, onOrganizerClick = onOrganizerClick)

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = pluralStringResource(
                    R.plurals.event_going_count,
                    event.goingCount,
                    event.goingCount
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(
            onClick = { onCardClick(event.id) },
            modifier = Modifier.testTag("eventMarkerCardDetail")
        ) {
            Text(text = stringResource(R.string.event_see_detail))
        }
    }
}

@Composable
private fun EventThumbnail(event: EventMapMarkerUi) {
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (event.imageUrl != null) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = stringResource(R.string.event_photo_cd, event.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(THUMBNAIL_SIZE)
            )
        } else {
            Icon(
                imageVector = event.type.cardIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(THUMBNAIL_SIZE / 2)
            )
        }
    }
}

@Composable
private fun EventTypeChip(type: EventType, isCancelled: Boolean) {
    val label = when {
        isCancelled -> stringResource(R.string.event_cancelled_label)
        type == EventType.CAR_MEET -> stringResource(R.string.event_type_car_meet)
        else -> stringResource(R.string.event_type_pique)
    }
    val color =
        if (isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun IconLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // el texto de al lado ya lo dice
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OrganizerRow(event: EventMapMarkerUi, onOrganizerClick: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        modifier = Modifier
            .clickable { onOrganizerClick(event.organizerId) }
            .testTag("eventMarkerCardOrganizer")
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (event.avatarUrl != null) {
                AsyncImage(
                    model = event.avatarUrl,
                    contentDescription = stringResource(R.string.event_avatar_cd, event.nombreOrganizador),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AVATAR_SIZE)
                )
            }
        }
        Text(
            text = event.nombreOrganizador,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val EventType.cardIcon
    get() = when (this) {
        EventType.CAR_MEET -> Icons.Filled.Groups
        EventType.PIQUE -> Icons.Filled.EmojiEvents
    }

private val THUMBNAIL_SIZE = 64.dp
private val AVATAR_SIZE = 20.dp

@Preview
@Composable
private fun EventMarkerCardPreview() {
    MotorMatesTheme {
        EventMarkerCard(
            event = EventMapMarkerUi(
                id = "evt-1",
                type = EventType.CAR_MEET,
                lat = 4.6280,
                lng = -74.0643,
                title = "Encuentro de clásicos en el Parque de la 93",
                descripcion = "Traigan sus clásicos. Habrá food trucks y premiación.",
                organizerId = "uid-ana",
                nombreOrganizador = "Ana Organiza",
                lugarTexto = "Parque de la 93",
                fechaTexto = "mañana, 7:00 p. m.",
                horarioTexto = "hasta 11:00 p. m.",
                features = listOf(EventFeature.EXHIBICION, EventFeature.FOOD_TRUCKS),
                goingCount = 24
            )
        )
    }
}

@Preview
@Composable
private fun EventMarkerCardCancelledPreview() {
    MotorMatesTheme {
        EventMarkerCard(
            event = EventMapMarkerUi(
                id = "evt-2",
                type = EventType.PIQUE,
                lat = 4.6280,
                lng = -74.0643,
                title = "Pique en el autódromo de Tocancipá",
                organizerId = "uid-ana",
                nombreOrganizador = "Ana Organiza",
                lugarTexto = "Autódromo de Tocancipá",
                fechaTexto = "sáb 22 nov, 9:00 a. m.",
                isCancelled = true,
                goingCount = 1
            )
        )
    }
}
