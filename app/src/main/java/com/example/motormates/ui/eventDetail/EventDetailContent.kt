package com.example.motormates.ui.eventDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventMapMarkerUi
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ParticipationStatus
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * El detalle de un evento, sin estado propio.
 */
@Composable
fun EventDetailContent(
    uiState: EventDetailUiState,
    modifier: Modifier = Modifier,
    onOrganizerClick: (String) -> Unit = {},
    onParticipationClick: (ParticipationStatus) -> Unit = {},
    onEditClick: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    val event = uiState.event ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .testTag("eventDetailScreen")
    ) {
        EventHeaderImage(event)

        Column(modifier = Modifier.padding(Spacing.gutter)) {
            EventBadges(event)

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            DetailLine(
                icon = Icons.Filled.Schedule,
                text = listOfNotNull(event.fechaTexto, event.horarioTexto).joinToString(" · ")
            )
            event.lugarTexto?.let { DetailLine(icon = Icons.Filled.LocationOn, text = it) }

            Spacer(modifier = Modifier.height(Spacing.md))

            OrganizerLine(event = event, onClick = { onOrganizerClick(event.organizerId) })

            event.descripcion?.let { descripcion ->
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (event.features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(
                        when (event.type) {
                            EventType.CAR_MEET -> R.string.new_event_features_car_meet
                            EventType.PIQUE -> R.string.new_event_features_pique
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                event.features.forEach { feature ->
                    DetailLine(icon = Icons.Filled.Check, text = stringResource(feature.labelRes))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            ParticipationCounts(event)

            if (uiState.canParticipate) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                ParticipationButtons(
                    shown = uiState.shownParticipation,
                    onClick = onParticipationClick
                )
            }

            if (event.isMine) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                OrganizerActions(
                    isCancelled = event.isCancelled,
                    isCancelling = uiState.isCancelling,
                    onEditClick = onEditClick,
                    onCancelClick = onCancelClick
                )
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("eventDetailError")
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun EventHeaderImage(event: EventMapMarkerUi) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (event.imageUrl != null) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = stringResource(R.string.event_photo_cd, event.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = when (event.type) {
                    EventType.CAR_MEET -> Icons.Filled.Groups
                    EventType.PIQUE -> Icons.Filled.EmojiEvents
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
private fun EventBadges(event: EventMapMarkerUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(
                when (event.type) {
                    EventType.CAR_MEET -> R.string.event_type_car_meet
                    EventType.PIQUE -> R.string.event_type_pique
                }
            ).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (event.isCancelled) {
            Text(
                text = stringResource(R.string.event_cancelled_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("eventDetailCancelledBadge")
            )
        } else if (event.hasFinished) {
            Text(
                text = stringResource(R.string.event_finished_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // el texto de al lado ya lo dice
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OrganizerLine(event: EventMapMarkerUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("eventDetailOrganizer"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
        Column {
            Text(
                text = stringResource(R.string.event_detail_organized_by),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = event.nombreOrganizador,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ParticipationCounts(event: EventMapMarkerUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = pluralStringResource(
                R.plurals.event_going_count,
                event.goingCount,
                event.goingCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("eventDetailGoingCount")
        )
        Text(
            text = pluralStringResource(
                R.plurals.event_interested_count,
                event.interestedCount,
                event.interestedCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("eventDetailInterestedCount")
        )
    }
}

/**
 * "Me interesa" y "Voy a asistir". Son dos estados excluyentes, así que
 * marcar uno quita el otro, y volver a tocar el que está marcado lo quita.
 */
@Composable
private fun ParticipationButtons(
    shown: ParticipationStatus?,
    onClick: (ParticipationStatus) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        ParticipationButton(
            label = stringResource(R.string.event_interested_action),
            icon = Icons.Filled.StarOutline,
            isSelected = shown == ParticipationStatus.INTERESTED,
            testTag = "eventDetailInterested",
            onClick = { onClick(ParticipationStatus.INTERESTED) },
            modifier = Modifier.weight(1f)
        )
        ParticipationButton(
            label = stringResource(R.string.event_going_action),
            icon = Icons.Filled.Check,
            isSelected = shown == ParticipationStatus.GOING,
            testTag = "eventDetailGoing",
            onClick = { onClick(ParticipationStatus.GOING) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ParticipationButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(Radius.md),
            modifier = modifier.testTag(testTag)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(Spacing.xxs))
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(Radius.md),
            modifier = modifier.testTag(testTag)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(Spacing.xxs))
            Text(text = label)
        }
    }
}

@Composable
private fun OrganizerActions(
    isCancelled: Boolean,
    isCancelling: Boolean,
    onEditClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onEditClick,
            shape = RoundedCornerShape(Radius.md),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eventDetailEdit")
        ) {
            Text(text = stringResource(R.string.event_detail_edit))
        }
        // Un evento ya cancelado no se vuelve a cancelar.
        if (!isCancelled) {
            TextButton(
                onClick = onCancelClick,
                enabled = !isCancelling,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eventDetailCancel")
            ) {
                Text(text = stringResource(R.string.event_detail_cancel))
            }
        }
    }
}

private val EventFeature.labelRes: Int
    get() = when (this) {
        EventFeature.EXHIBICION -> R.string.event_feature_exhibicion
        EventFeature.ZONA_FOTOS -> R.string.event_feature_zona_fotos
        EventFeature.FOOD_TRUCKS -> R.string.event_feature_food_trucks
        EventFeature.MUSICA -> R.string.event_feature_musica
        EventFeature.PREMIACION -> R.string.event_feature_premiacion
        EventFeature.APTO_FAMILIAS -> R.string.event_feature_apto_familias
        EventFeature.PARQUEADERO -> R.string.event_feature_parqueadero
        EventFeature.ENTRADA_LIBRE -> R.string.event_feature_entrada_libre
        EventFeature.CRONOMETRAJE -> R.string.event_feature_cronometraje
        EventFeature.PISTA_CERRADA -> R.string.event_feature_pista_cerrada
        EventFeature.CASCO_OBLIGATORIO -> R.string.event_feature_casco
        EventFeature.ARNES_OBLIGATORIO -> R.string.event_feature_arnes
        EventFeature.EXTINTOR_ABORDO -> R.string.event_feature_extintor
        EventFeature.JAULA_ANTIVUELCO -> R.string.event_feature_jaula
        EventFeature.AMBULANCIA_EN_SITIO -> R.string.event_feature_ambulancia
        EventFeature.INSCRIPCION_PREVIA -> R.string.event_feature_inscripcion
    }

private val HEADER_HEIGHT = 180.dp
private val AVATAR_SIZE = 36.dp
