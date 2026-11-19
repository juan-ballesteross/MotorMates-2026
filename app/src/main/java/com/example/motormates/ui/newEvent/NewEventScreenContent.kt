package com.example.motormates.ui.newEvent

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.data.model.EventType
import com.example.motormates.ui.newEvent.components.EventDateTimeField
import com.example.motormates.ui.newEvent.components.EventFeatureCheckboxes
import com.example.motormates.ui.newEvent.components.EventLocationRow
import com.example.motormates.ui.newEvent.components.EventTypeSelector
import com.example.motormates.ui.newEvent.components.NewEventTextField
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * El formulario de evento, sin estado propio.
 *
 * Un solo formulario para Car Meets y piques: lo único que cambia es la
 * lista de características, que llega ya filtrada por el tipo elegido.
 */
@Composable
fun NewEventScreenContent(
    uiState: NewEventUiState,
    modifier: Modifier = Modifier,
    onTypeChange: (EventType) -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onVenueChange: (String) -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onToggleFeature: (com.example.motormates.data.model.EventFeature) -> Unit = {},
    onPickLocationClick: () -> Unit = {},
    onStartAtChange: (Long) -> Unit = {},
    onEndAtChange: (Long) -> Unit = {},
    onClearEndAt: () -> Unit = {},
    onAddPhotoClick: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter)
            .testTag("newEventScreen")
    ) {
        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = stringResource(R.string.new_event_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        EventTypeSelector(selected = uiState.type, onSelect = onTypeChange)

        Spacer(modifier = Modifier.height(Spacing.lg))

        NewEventTextField(
            label = stringResource(R.string.new_event_name_label),
            value = uiState.title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.new_event_name_placeholder),
            maxLength = TITLE_MAX_LENGTH,
            testTag = "newEventTitle"
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        NewEventTextField(
            label = stringResource(R.string.new_event_description_label),
            value = uiState.description,
            onValueChange = onDescriptionChange,
            placeholder = stringResource(R.string.new_event_description_placeholder),
            minLines = 3,
            maxLength = DESCRIPTION_MAX_LENGTH,
            testTag = "newEventDescription"
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.new_event_location_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        EventLocationRow(location = uiState.location, onClick = onPickLocationClick)

        Spacer(modifier = Modifier.height(Spacing.md))

        NewEventTextField(
            label = stringResource(R.string.new_event_venue_label),
            value = uiState.venueName,
            onValueChange = onVenueChange,
            placeholder = stringResource(R.string.new_event_venue_placeholder),
            testTag = "newEventVenue"
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        NewEventTextField(
            label = stringResource(R.string.new_event_address_label),
            value = uiState.address,
            onValueChange = onAddressChange,
            placeholder = stringResource(R.string.new_event_address_placeholder),
            testTag = "newEventAddress"
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        EventDateTimeField(
            label = stringResource(R.string.new_event_start_label),
            valueMillis = uiState.startAt,
            onValueChange = onStartAtChange,
            testTag = "newEventStart"
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        EventDateTimeField(
            label = stringResource(R.string.new_event_end_label),
            valueMillis = uiState.endAt,
            onValueChange = onEndAtChange,
            // No deja elegir un día anterior al inicio. La validación de
            // verdad está en el ViewModel, que sí puede explicar el error.
            minMillis = uiState.startAt,
            onClear = onClearEndAt,
            testTag = "newEventEnd"
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(
                when (uiState.type) {
                    EventType.CAR_MEET -> R.string.new_event_features_car_meet
                    EventType.PIQUE -> R.string.new_event_features_pique
                }
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EventFeatureCheckboxes(
            available = uiState.availableFeatures,
            selected = uiState.selectedFeatures,
            onToggle = onToggleFeature
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        EventPhotoRow(
            imageUri = uiState.imageUri,
            existingImageUrl = uiState.existingImageUrl,
            onAddClick = onAddPhotoClick,
            onRemoveClick = onRemovePhoto
        )

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("newEventError")
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Button(
            onClick = onSaveClick,
            enabled = uiState.canPublish,
            shape = RoundedCornerShape(Radius.md),
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .testTag("newEventSave")
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stringResource(
                        if (uiState.isEditing) R.string.new_event_save
                        else R.string.new_event_publish
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

@Composable
private fun EventPhotoRow(
    imageUri: Uri?,
    existingImageUrl: String?,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val model: Any? = imageUri ?: existingImageUrl

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.new_event_photo_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (model != null) {
                Box {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(PHOTO_SIZE)
                            .clip(RoundedCornerShape(Radius.md))
                    )
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("newEventRemovePhoto")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.new_event_change_photo),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(PHOTO_SIZE)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onAddClick)
                        .testTag("newEventAddPhoto"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.new_event_add_photo),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private const val TITLE_MAX_LENGTH = 80
private const val DESCRIPTION_MAX_LENGTH = 500
private val PHOTO_SIZE = 96.dp
private val BUTTON_HEIGHT = 56.dp
