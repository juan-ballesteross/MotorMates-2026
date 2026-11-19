package com.example.motormates.ui.newEvent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.data.util.combineToEpochMillis
import com.example.motormates.data.util.toDatePickerMillis
import com.example.motormates.data.util.toEventDateTimeEs
import com.example.motormates.data.util.toLocalHourMinute
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Fecha y hora en un solo control: una fila con el valor ya formateado que,
 * al tocarla, abre primero el calendario y después el reloj.
 *
 * Son dos diálogos encadenados porque Material 3 **no trae** un selector
 * combinado de fecha y hora. Todo queda contenido en este archivo para que,
 * el día que aparezca una API estable, solo cambie aquí.
 *
 * Es el primer selector de fecha del proyecto, así que opta por la API
 * experimental de M3; ya hay precedente de esa opción en ClusterReviewsSheet.
 *
 * @param minMillis para el campo de fin: deshabilita los días anteriores al
 *   inicio. La validación de verdad está en el ViewModel, que es quien puede
 *   explicar el error; esto solo evita que el usuario llegue hasta ahí.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateTimeField(
    label: String,
    valueMillis: Long?,
    onValueChange: (Long) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    minMillis: Long? = null,
    onClear: (() -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xxs)
                .clip(RoundedCornerShape(Radius.md))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showDatePicker = true }
                .padding(Spacing.sm)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null, // el label de arriba ya lo dice
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = valueMillis?.toEventDateTimeEs()
                    ?: stringResource(R.string.new_event_pick_datetime),
                style = MaterialTheme.typography.bodyLarge,
                color = if (valueMillis == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            if (valueMillis != null && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.testTag("${testTag}Clear")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.new_event_clear_end),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = valueMillis?.toDatePickerMillis(),
            selectableDates = minMillis?.let { min ->
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis >= min.toDatePickerMillis()
                }
            } ?: object : SelectableDates {}
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // No se cierra el flujo aquí: elegir el día solo abre
                        // el reloj. El valor no cambia hasta confirmar la hora.
                        pendingDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null,
                    modifier = Modifier.testTag("${testTag}DateConfirm")
                ) {
                    Text(text = stringResource(R.string.new_event_next))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    pendingDateMillis?.let { dateMillis ->
        val initial = valueMillis?.toLocalHourMinute() ?: (DEFAULT_HOUR to 0)
        val timePickerState = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { pendingDateMillis = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        // El cálculo delicado: el calendario da medianoche UTC
                        // y el reloj da hora local. Ver combineToEpochMillis.
                        onValueChange(
                            combineToEpochMillis(
                                dateUtcMillis = dateMillis,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )
                        )
                        pendingDateMillis = null
                    },
                    modifier = Modifier.testTag("${testTag}TimeConfirm")
                ) {
                    Text(text = stringResource(R.string.common_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDateMillis = null }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

/** Las 7 p. m.: la hora a la que de verdad arrancan estos encuentros. */
private const val DEFAULT_HOUR = 19
