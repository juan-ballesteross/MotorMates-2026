package com.example.motormates.ui.eventDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.ParticipationStatus
import com.example.motormates.data.model.toEventMapMarkerUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.toEventDateTimeEs
import com.example.motormates.data.util.toEventTimeEs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El detalle de un evento, con la participación del usuario.
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState

    private var participationJob: Job? = null

    fun start(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loadEvent(eventId)
        }
        observeParticipation(eventId)
    }

    fun reload(eventId: String) {
        viewModelScope.launch { loadEvent(eventId) }
    }

    private suspend fun loadEvent(eventId: String) {
        eventRepository.getEventById(eventId).fold(
            onSuccess = { event ->
                val marker = event.toEventMapMarkerUi(
                    currentUserId = authRepository.getCurrentUser()?.id,
                    nowMillis = timeSource.nowMillis(),
                    fechaTexto = event.startAt.toEventDateTimeEs(),
                    horarioTexto = event.endAt?.let { fin -> "hasta ${fin.toEventTimeEs()}" }
                )
                _uiState.update { it.copy(event = marker, isLoading = false, errorMessage = null) }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: LOAD_FAILED)
                }
            }
        )
    }

    /**
     * El documento de participación en vivo. Cuando el servidor confirma, se
     * descarta lo pendiente: a partir de ahí manda lo confirmado.
     */
    private fun observeParticipation(eventId: String) {
        participationJob?.cancel()
        participationJob = viewModelScope.launch {
            eventRepository.observeMyParticipation(eventId).collect { status ->
                _uiState.update {
                    it.copy(
                        myParticipation = status,
                        hasPendingParticipation = false,
                        pendingParticipation = null
                    )
                }
            }
        }
    }

    /**
     * Marca o desmarca una respuesta. Volver a tocar la que ya estaba la
     * quita, que es lo que espera cualquiera de un botón así.
     *
     * Es optimista: se pinta el cambio y, si la escritura falla, se revierte
     * y se explica. Los contadores NO se mueven a mano: vienen del documento
     * del evento, y moverlos aquí dejaría dos verdades (mismo criterio que
     * toggleFollow en PublicProfileViewModel).
     */
    fun toggleParticipation(status: ParticipationStatus) {
        val state = _uiState.value
        val eventId = state.event?.id ?: return
        if (!state.canParticipate) return

        val next = if (state.shownParticipation == status) null else status
        _uiState.update {
            it.copy(
                pendingParticipation = next,
                hasPendingParticipation = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            eventRepository.setParticipation(eventId, next).onFailure { error ->
                _uiState.update {
                    it.copy(
                        pendingParticipation = null,
                        hasPendingParticipation = false,
                        errorMessage = error.message ?: PARTICIPATION_FAILED
                    )
                }
            }
            // En caso de éxito no se toca nada: el listener de participación
            // confirma y limpia lo pendiente.
        }
    }

    // ---------- Acciones del organizador ----------

    fun askCancelEvent() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun confirmCancelEvent() {
        val state = _uiState.value
        val eventId = state.event?.id ?: return
        if (state.isCancelling) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, showCancelDialog = false) }
            eventRepository.cancelEvent(eventId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isCancelling = false, isCancelled = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            errorMessage = error.message ?: CANCEL_FAILED
                        )
                    }
                }
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private companion object {
        const val LOAD_FAILED = "No se pudo cargar el evento"
        const val PARTICIPATION_FAILED = "No se pudo guardar tu respuesta"
        const val CANCEL_FAILED = "No se pudo cancelar el evento"
    }
}
