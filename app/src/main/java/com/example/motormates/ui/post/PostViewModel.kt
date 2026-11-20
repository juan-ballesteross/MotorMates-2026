package com.example.motormates.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.model.VehicleFormField
import com.example.motormates.data.model.VehicleFormInput
import com.example.motormates.data.model.toFormInput
import com.example.motormates.data.model.validate
import com.example.motormates.data.repository.GarageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Formulario para publicar un vehículo propio, o para editarlo si se llama
 * startEditing. Queda guardado con el usuario como dueño (ownerId), así
 * aparece en su garaje y se puede comparar con "Comparar con el mío".
 */
@HiltViewModel
class PostViewModel @Inject constructor(
    private val garageRepository: GarageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    /**
     * Modo edición: carga el vehículo y prellena el formulario. Se llama desde
     * un LaunchedEffect; al rotar no se vuelve a cargar (se perderían los
     * cambios sin guardar).
     */
    fun startEditing(vehicleId: String) {
        if (_uiState.value.editingVehicleId == vehicleId) return
        _uiState.value = PostUiState(editingVehicleId = vehicleId)
        loadVehicle(vehicleId)
    }

    fun retryLoad() {
        _uiState.value.editingVehicleId?.let(::loadVehicle)
    }

    private fun loadVehicle(vehicleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVehicle = true, loadError = null) }
            garageRepository.getMyVehicle(vehicleId).fold(
                onSuccess = { vehicle ->
                    _uiState.update {
                        it.copy(
                            isLoadingVehicle = false,
                            form = vehicle.toFormInput(),
                            existingImageUrl = vehicle.imageUrl
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingVehicle = false,
                            loadError = error.message ?: "No se pudo cargar el vehículo"
                        )
                    }
                }
            )
        }
    }

    fun onBrandChange(value: String) = updateForm(VehicleFormField.BRAND) { it.copy(brand = value) }

    fun onModelChange(value: String) = updateForm(VehicleFormField.MODEL) { it.copy(model = value) }

    /** Solo dígitos y máximo 4: el teclado numérico igual deja pegar texto. */
    fun onYearChange(value: String) =
        updateForm(VehicleFormField.YEAR) { it.copy(year = value.filter(Char::isDigit).take(4)) }

    /** Tocar la categoría elegida la quita. */
    fun onCategorySelect(category: String) = updateForm(VehicleFormField.CATEGORY) {
        it.copy(category = if (it.category == category) null else category)
    }

    fun onPotenciaChange(value: String) =
        updateForm(VehicleFormField.POTENCIA) { it.copy(potenciaHp = value.filter(Char::isDigit).take(4)) }

    fun onAceleracionChange(value: String) = updateForm(VehicleFormField.ACELERACION) {
        it.copy(aceleracionS = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5))
    }

    fun onVelocidadChange(value: String) =
        updateForm(VehicleFormField.VELOCIDAD) { it.copy(velocidadKmh = value.filter(Char::isDigit).take(3)) }

    fun onTraccionSelect(traccion: String) = updateForm(null) {
        it.copy(traccion = if (it.traccion == traccion) null else traccion)
    }

    fun onImagePicked(uri: Uri?) {
        // null = el usuario cerró la galería sin elegir: se deja la foto que había.
        if (uri != null) _uiState.update { it.copy(imageUri = uri) }
    }

    /** Quita la foto nueva y también la que ya tenía (al guardar queda sin foto). */
    fun removeImage() {
        _uiState.update { it.copy(imageUri = null, existingImageUrl = null) }
    }

    fun publish() {
        val current = _uiState.value
        if (current.isPublishing) return

        val validation = current.form.validate(currentYear = Year.now().value)
        val draft = validation.draft
        if (draft == null) {
            _uiState.update { it.copy(errors = validation.errors) }
            return
        }

        val editingId = current.editingVehicleId
        if (editingId != null) {
            saveChanges(editingId, draft, current)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true, errors = emptyMap(), errorMessage = null) }

            garageRepository.publishVehicle(draft, current.imageUri).fold(
                onSuccess = { vehicle ->
                    // Se limpia el formulario: si vuelve a la pestaña, arranca de cero.
                    _uiState.value = PostUiState(publishedVehicleId = vehicle.id)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            errorMessage = error.message ?: "No se pudo publicar el vehículo"
                        )
                    }
                }
            )
        }
    }

    private fun saveChanges(vehicleId: String, draft: VehicleDraft, current: PostUiState) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true, errors = emptyMap(), errorMessage = null) }

            garageRepository.updateVehicle(
                vehicleId = vehicleId,
                draft = draft,
                newImageUri = current.imageUri,
                keepImageUrl = current.existingImageUrl
            ).fold(
                // El formulario se queda como está: la pantalla se cierra sola.
                onSuccess = { _uiState.update { it.copy(isPublishing = false, publishedVehicleId = vehicleId) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            errorMessage = error.message ?: "No se pudieron guardar los cambios"
                        )
                    }
                }
            )
        }
    }

    // ---------- Eliminar (solo al editar) ----------

    fun askDelete() {
        if (_uiState.value.isEditing) _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        val vehicleId = _uiState.value.editingVehicleId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirm = false, isDeleting = true, errorMessage = null) }
            garageRepository.deleteVehicle(vehicleId).fold(
                onSuccess = { _uiState.update { it.copy(isDeleting = false, wasDeleted = true) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isDeleting = false, errorMessage = error.message ?: "No se pudo eliminar el vehículo")
                    }
                }
            )
        }
    }

    fun onDeletedHandled() {
        _uiState.update { it.copy(wasDeleted = false) }
    }

    fun onPublishedHandled() {
        _uiState.update { it.copy(publishedVehicleId = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Cambia el formulario y borra el error del campo que se está corrigiendo. */
    private fun updateForm(field: VehicleFormField?, change: (VehicleFormInput) -> VehicleFormInput) {
        _uiState.update { state ->
            state.copy(
                form = change(state.form),
                errors = if (field == null) state.errors else state.errors - field
            )
        }
    }
}
