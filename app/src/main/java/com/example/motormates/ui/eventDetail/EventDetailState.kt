package com.example.motormates.ui.eventDetail

import com.example.motormates.data.model.EventMapMarkerUi
import com.example.motormates.data.model.ParticipationStatus

/**
 * El detalle de un evento.
 *
 * Reutiliza [EventMapMarkerUi] en vez de un modelo propio: ya trae todo lo
 * que esta pantalla muestra (textos formateados, isMine, isCancelled,
 * hasFinished) y lo produce el mismo mapper, así que no hay dos verdades.
 */
data class EventDetailUiState(
    val event: EventMapMarkerUi? = null,
    /** Lo que el servidor confirma. */
    val myParticipation: ParticipationStatus? = null,
    /**
     * Lo que el usuario acaba de tocar, antes de que el servidor confirme.
     * Mientras no sea null, manda sobre [myParticipation]: es la respuesta
     * inmediata que espera quien tocó el botón. Si la escritura falla, se
     * descarta y vuelve a verse el estado real.
     */
    val pendingParticipation: ParticipationStatus? = null,
    /** True mientras hay un cambio de participación sin confirmar. */
    val hasPendingParticipation: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCancelDialog: Boolean = false,
    val isCancelling: Boolean = false,
    /** La pantalla reacciona a esto para cerrarse tras cancelar el evento. */
    val isCancelled: Boolean = false
) {
    /** Lo que se pinta: lo pendiente si hay, y si no lo confirmado. */
    val shownParticipation: ParticipationStatus?
        get() = if (hasPendingParticipation) pendingParticipation else myParticipation

    /** Un evento cancelado o que ya pasó no admite nuevas respuestas. */
    val canParticipate: Boolean
        get() = event != null && !event.isCancelled && !event.hasFinished
}
