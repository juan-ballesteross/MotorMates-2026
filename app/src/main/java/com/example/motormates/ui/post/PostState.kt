package com.example.motormates.ui.post

import android.net.Uri
import com.example.motormates.data.model.VehicleFormField
import com.example.motormates.data.model.VehicleFormInput

/**
 * Formulario de vehículo. Sirve para publicar uno nuevo (pestaña Publicar)
 * y para editar uno propio (desde Editar perfil): editingVehicleId lo decide.
 */
data class PostUiState(
    val form: VehicleFormInput = VehicleFormInput(),
    /** Foto nueva elegida de la galería (opcional). */
    val imageUri: Uri? = null,
    /** Al editar: la foto que ya tenía el vehículo. Se conserva si no se elige otra. */
    val existingImageUrl: String? = null,
    /** Errores por campo; solo aparecen después de intentar guardar. */
    val errors: Map<VehicleFormField, String> = emptyMap(),
    val isPublishing: Boolean = false,
    val errorMessage: String? = null,
    /** Evento de una sola vez: el id del vehículo publicado o editado, para navegar. */
    val publishedVehicleId: String? = null,

    // ---------- Solo al editar ----------
    val editingVehicleId: String? = null,
    val isLoadingVehicle: Boolean = false,
    /** No se pudo abrir el vehículo (no existe, no es tuyo, sin conexión). */
    val loadError: String? = null,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    /** Evento de una sola vez: el vehículo se eliminó, hay que cerrar la pantalla. */
    val wasDeleted: Boolean = false
) {
    val isEditing: Boolean get() = editingVehicleId != null

    /** Lo que se muestra en el recuadro de la foto: la nueva o, si no hay, la que ya tenía. */
    val photoModel: Any? get() = imageUri ?: existingImageUrl

    /** Lo mínimo para habilitar el botón; la validación completa se hace al tocarlo. */
    val canPublish: Boolean
        get() = !isPublishing &&
            !isLoadingVehicle &&
            !isDeleting &&
            loadError == null &&
            form.brand.isNotBlank() &&
            form.model.isNotBlank() &&
            form.year.isNotBlank() &&
            form.category != null
}
