package com.example.motormates.ui.newEvent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.R
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.ui.review.components.NewReviewTopBar

/**
 * Crear o editar un Car Meet o un pique.
 *
 * El punto en el mapa se elige en otra pantalla ([PickLocationScreen]) y
 * vuelve por [pickedLocation]: el ViewModel de esta pantalla sigue vivo
 * mientras el picker está encima, así que lo escrito no se pierde.
 *
 * @param pickedLocation el punto que devolvió el picker, o null. Se consume
 *   una vez (ver [onPickedLocationHandled]) para que volver a entrar no
 *   reaplique un punto viejo.
 */
@Composable
fun NewEventScreen(
    eventId: String?,
    onCloseClick: () -> Unit,
    onSaved: () -> Unit,
    onPickLocationClick: (lat: Double?, lng: Double?) -> Unit,
    pickedLocation: ReviewLocation? = null,
    onPickedLocationHandled: () -> Unit = {},
    viewModel: NewEventViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) { viewModel.start(eventId) }

    LaunchedEffect(pickedLocation) {
        val picked = pickedLocation ?: return@LaunchedEffect
        viewModel.updateLocation(picked)
        onPickedLocationHandled()
    }

    // La pantalla no decide cuándo cerrarse: reacciona a que el estado diga
    // que ya se guardó. Mismo patrón que NewReviewScreen.
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onSaved() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::updateImage) }

    Column(modifier = modifier.fillMaxSize()) {
        // Se reutiliza la barra de nueva reseña: ya acepta el título por
        // parámetro y su botón de cerrar dice simplemente "Cerrar".
        NewReviewTopBar(
            onCloseClick = onCloseClick,
            title = stringResource(
                if (uiState.isEditing) R.string.new_event_title_edit
                else R.string.new_event_title_create
            )
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            // Solo al editar: si el evento no se pudo cargar no hay formulario
            // que mostrar. Al crear, un error de guardado se muestra dentro.
            uiState.isEditing && uiState.errorMessage != null && uiState.startAt == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.new_event_load_error),
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> NewEventScreenContent(
                uiState = uiState,
                onTypeChange = viewModel::updateType,
                onTitleChange = viewModel::updateTitle,
                onDescriptionChange = viewModel::updateDescription,
                onVenueChange = viewModel::updateVenueName,
                onAddressChange = viewModel::updateAddress,
                onToggleFeature = viewModel::toggleFeature,
                onPickLocationClick = {
                    onPickLocationClick(uiState.location?.lat, uiState.location?.lng)
                },
                onStartAtChange = viewModel::updateStartAt,
                onEndAtChange = viewModel::updateEndAt,
                onClearEndAt = viewModel::clearEndAt,
                onAddPhotoClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePhoto = { viewModel.updateImage(null) },
                onSaveClick = viewModel::save
            )
        }
    }
}
