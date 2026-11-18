package com.example.motormates.ui.newEvent

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.repository.EventDraft
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.util.TimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Crear o editar un Car Meet o un pique.
 *
 * Un solo ViewModel para los dos tipos: lo único que cambia entre ellos son
 * las características marcables, y el propio enum sabe a cuál pertenece cada
 * una (ver [EventFeature]).
 */
@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewEventUiState())
    val uiState: StateFlow<NewEventUiState> = _uiState

    /**
     * Carga el evento al editar. Idempotente a propósito: la pantalla la
     * llama desde un LaunchedEffect, y recomponer no debe volver a pedir el
     * documento ni pisar lo que el usuario ya cambió.
     */
    fun start(eventId: String?) {
        if (eventId == null || _uiState.value.eventId == eventId) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            eventRepository.getEventById(eventId).fold(
                onSuccess = { event ->
                    _uiState.update {
                        it.copy(
                            eventId = event.id,
                            type = event.type,
                            title = event.title,
                            description = event.description.orEmpty(),
                            venueName = event.venueName.orEmpty(),
                            address = event.address.orEmpty(),
                            location = event.location,
                            startAt = event.startAt,
                            endAt = event.endAt,
                            selectedFeatures = event.features,
                            existingImageUrl = event.imageUrl,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: LOAD_FAILED)
                    }
                }
            )
        }
    }

    // ---------- Campos ----------

    /**
     * Cambiar el tipo descarta las características del tipo anterior. Sin
     * esto, un "food trucks" marcado para un Car Meet sobreviviría al
     * convertirlo en pique, donde ni siquiera se muestra.
     */
    fun updateType(type: EventType) {
        _uiState.update { state ->
            state.copy(
                type = type,
                selectedFeatures = state.selectedFeatures.filterTo(mutableSetOf()) { it.type == type },
                errorMessage = null
            )
        }
    }

    fun updateTitle(input: String) {
        _uiState.update { it.copy(title = input, errorMessage = null) }
    }

    fun updateDescription(input: String) {
        _uiState.update { it.copy(description = input) }
    }

    fun updateVenueName(input: String) {
        _uiState.update { it.copy(venueName = input) }
    }

    fun updateAddress(input: String) {
        _uiState.update { it.copy(address = input) }
    }

    fun toggleFeature(feature: EventFeature) {
        _uiState.update { state ->
            val next = if (feature in state.selectedFeatures) {
                state.selectedFeatures - feature
            } else {
                state.selectedFeatures + feature
            }
            state.copy(selectedFeatures = next)
        }
    }

    fun updateStartAt(millis: Long) {
        _uiState.update { state ->
            state.copy(
                startAt = millis,
                // Un fin que queda antes del nuevo inicio deja de tener
                // sentido: se borra en vez de dejar un rango imposible.
                endAt = state.endAt?.takeIf { it > millis },
                errorMessage = null
            )
        }
    }

    fun updateEndAt(millis: Long) {
        _uiState.update { it.copy(endAt = millis, errorMessage = null) }
    }

    fun clearEndAt() {
        _uiState.update { it.copy(endAt = null, errorMessage = null) }
    }

    /**
     * El punto elegido en el mapa. Prellena el nombre del sitio solo si el
     * usuario no ha escrito uno: lo que él escribe nunca se pisa.
     */
    fun updateLocation(location: ReviewLocation) {
        _uiState.update { state ->
            state.copy(
                location = location,
                venueName = state.venueName.ifBlank { location.placeName.orEmpty() },
                errorMessage = null
            )
        }
    }

    fun updateImage(uri: Uri?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    // ---------- Guardar ----------

    fun save() {
        val current = _uiState.value
        // Mismo guardia que NewReviewViewModel: sin esto, dos toques rápidos
        // publican dos eventos.
        if (current.isSaving) return

        val startAt = current.startAt
        val location = current.location
        when {
            current.title.isBlank() -> return fail("El evento necesita un título")
            startAt == null -> return fail("Elige la fecha y la hora de inicio")
            location == null -> return fail("Elige en el mapa dónde será el evento")
            current.endAt != null && current.endAt <= startAt ->
                return fail("La hora de fin debe ser después de la de inicio")
            // Solo al crear: un evento ya publicado se puede seguir editando
            // después de su fecha, por ejemplo para corregir un dato.
            !current.isEditing && startAt < timeSource.nowMillis() ->
                return fail("La fecha de inicio ya pasó")
        }

        // Se marca ANTES de lanzar, no dentro de la corrutina. Si se hiciera
        // dentro, el guardia de arriba no serviría de nada con un
        // dispatcher que encola en vez de ejecutar en el momento: las dos
        // llamadas verían isSaving en false y se publicaría dos veces.
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val draft = EventDraft(
                type = current.type,
                title = current.title,
                description = current.description.ifBlank { null },
                location = location,
                venueName = current.venueName.ifBlank { null },
                address = current.address.ifBlank { null },
                startAt = startAt,
                endAt = current.endAt,
                features = current.selectedFeatures,
                imageUri = current.imageUri,
                existingImageUrl = current.existingImageUrl
            )

            val result = current.eventId
                ?.let { eventRepository.updateEvent(it, draft) }
                ?: eventRepository.createEvent(draft)

            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isSaving = false, isSaved = true) },
                    onFailure = { error ->
                        state.copy(isSaving = false, errorMessage = error.message ?: SAVE_FAILED)
                    }
                )
            }
        }
    }

    private fun fail(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private companion object {
        const val LOAD_FAILED = "No se pudo cargar el evento"
        const val SAVE_FAILED = "No se pudo guardar el evento"
    }
}
