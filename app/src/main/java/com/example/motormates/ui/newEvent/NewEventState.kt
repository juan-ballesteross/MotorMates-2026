package com.example.motormates.ui.newEvent

import android.net.Uri
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ReviewLocation

/**
 * El formulario de crear o editar un evento.
 *
 * Plano y con todos los campos por defecto, como el resto de la app: no hay
 * jerarquías de estado ni objetos de error por campo. La validación escribe
 * un mensaje en [errorMessage], que es lo que hace NewReviewViewModel.
 */
data class NewEventUiState(
    /** Null = se está creando; con valor = se está editando ese evento. */
    val eventId: String? = null,
    val type: EventType = EventType.CAR_MEET,
    val title: String = "",
    val description: String = "",
    val venueName: String = "",
    val address: String = "",
    val location: ReviewLocation? = null,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val selectedFeatures: Set<EventFeature> = emptySet(),
    val imageUri: Uri? = null,
    /** La que ya estaba publicada, al editar sin cambiar la foto. */
    val existingImageUrl: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** La pantalla reacciona a esto para cerrarse; no decide ella cuándo. */
    val isSaved: Boolean = false
) {
    val isEditing: Boolean get() = eventId != null

    /**
     * Las características del tipo elegido. Un solo enum con el tipo dentro
     * es lo que permite que un único composable de casillas sirva para Car
     * Meets y para piques.
     */
    val availableFeatures: List<EventFeature>
        get() = EventFeature.entries.filter { it.type == type }

    /**
     * Lo mínimo para publicar. El resto de las validaciones (que el fin sea
     * después del inicio, que la fecha no haya pasado) se revisan al guardar,
     * porque ahí se puede explicar qué está mal.
     */
    val canPublish: Boolean
        get() = title.isNotBlank() && startAt != null && location != null && !isSaving
}
