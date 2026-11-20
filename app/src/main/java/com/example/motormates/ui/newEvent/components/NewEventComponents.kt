package com.example.motormates.ui.newEvent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Car Meet o pique. Dos pastillas y no un desplegable: en esta app no existe
 * ningún dropdown, y con dos opciones un selector visible se entiende mejor.
 */
@Composable
fun EventTypeSelector(
    selected: EventType,
    onSelect: (EventType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        EventType.entries.forEach { type ->
            val isSelected = type == selected
            Text(
                text = stringResource(type.labelRes),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                // El Text ocupa el ancho entero de la pastilla (weight), asi
                // que sin esto la etiqueta queda pegada al borde izquierdo.
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .then(
                        if (isSelected) {
                            Modifier
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(Radius.xl)
                            )
                        }
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelect(type) }
                    .padding(vertical = 11.dp)
                    .testTag("newEventType_${type.name.lowercase()}")
            )
        }
    }
}

/**
 * Las características marcables del tipo elegido.
 *
 * Un solo composable sirve para los dos tipos porque la lista llega ya
 * filtrada: [EventFeature] sabe a qué tipo pertenece cada constante. El
 * estado es un Set con una función de alternar, igual que ReviewAspectChips.
 *
 * Son casillas y no pastillas porque aquí se marcan requisitos y servicios
 * ("casco obligatorio", "entrada libre"), y una casilla comunica mejor que
 * son varias cosas independientes que se activan o no.
 */
@Composable
fun EventFeatureCheckboxes(
    available: List<EventFeature>,
    selected: Set<EventFeature>,
    onToggle: (EventFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        available.forEach { feature ->
            val isChecked = feature in selected
            val label = stringResource(feature.labelRes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(feature) }
                    // Una sola parada del lector de pantalla por fila, en vez
                    // de "casilla" y el texto por separado.
                    .clearAndSetSemantics { }
                    .testTag("newEventFeature_${feature.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isChecked, onCheckedChange = { onToggle(feature) })
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Campo de texto con etiqueta, al estilo de EditProfileBioField. */
@Composable
fun NewEventTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 1,
    maxLength: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (maxLength == null || input.length <= maxLength) onValueChange(input)
            },
            placeholder = placeholder?.let { { Text(text = it) } },
            minLines = minLines,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xxs)
                .testTag(testTag)
        )
        maxLength?.let {
            Text(
                text = "${value.length}/$it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * La fila que abre el mapa para elegir el punto. Muestra el sitio ya elegido
 * para que se vea que el dato está puesto sin tener que volver a entrar.
 */
@Composable
fun EventLocationRow(
    location: ReviewLocation?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(Spacing.sm)
            .testTag("newEventLocation"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = if (location == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = location?.placeName
                ?: location?.let { stringResource(R.string.new_event_location_picked) }
                ?: stringResource(R.string.new_event_pick_location),
            style = MaterialTheme.typography.bodyLarge,
            color = if (location == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private val EventType.labelRes: Int
    get() = when (this) {
        EventType.CAR_MEET -> R.string.event_type_car_meet
        EventType.PIQUE -> R.string.event_type_pique
    }

/**
 * Las etiquetas viven aquí y no en el enum, igual que con FeedTab y
 * ReviewAspect: así data/model no importa recursos de Android.
 */
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
