package com.example.motormates.ui.eventDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.R
import com.example.motormates.ui.review.components.NewReviewTopBar

/**
 * El detalle de un evento: toda su información, la participación propia y,
 * si se es el organizador, editar y cancelar.
 *
 * @param reloadSignal lo pone el grafo de navegación cuando se vuelve de
 *   editar. Sin esto, el LaunchedEffect(eventId) no se vuelve a disparar y el
 *   detalle mostraría los datos de antes de la edición.
 */
@Composable
fun EventDetailScreen(
    eventId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onOrganizerClick: (String) -> Unit,
    reloadSignal: Boolean = false,
    onReloadHandled: () -> Unit = {},
    viewModel: EventDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) { viewModel.start(eventId) }

    LaunchedEffect(reloadSignal) {
        if (reloadSignal) {
            viewModel.reload(eventId)
            onReloadHandled()
        }
    }

    // Cancelar el evento cierra la pantalla: ya no hay nada que responder.
    LaunchedEffect(uiState.isCancelled) { if (uiState.isCancelled) onBackClick() }

    Column(modifier = modifier.fillMaxSize()) {
        NewReviewTopBar(
            onCloseClick = onBackClick,
            title = stringResource(R.string.event_detail_title)
        )

        when {
            uiState.event == null && uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.event == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.event_detail_load_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("eventDetailLoadError")
                )
            }

            else -> EventDetailContent(
                uiState = uiState,
                onOrganizerClick = onOrganizerClick,
                onParticipationClick = viewModel::toggleParticipation,
                onEditClick = { onEditClick(eventId) },
                onCancelClick = viewModel::askCancelEvent
            )
        }
    }

    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCancelDialog,
            title = { Text(text = stringResource(R.string.event_cancel_dialog_title)) },
            text = { Text(text = stringResource(R.string.event_cancel_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmCancelEvent,
                    modifier = Modifier.testTag("eventCancelConfirm")
                ) {
                    Text(text = stringResource(R.string.event_cancel_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCancelDialog) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
            modifier = Modifier.testTag("eventCancelDialog")
        )
    }
}
