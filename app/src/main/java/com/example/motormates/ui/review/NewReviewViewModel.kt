package com.example.motormates.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.CURRENT_USER_ID
import com.example.motormates.data.model.ReviewAspect
import com.example.motormates.data.model.toReviewCarSummary
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.navigation.ScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewReviewViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewReviewUiState())
    val uiState: StateFlow<NewReviewUiState> = _uiState

    /**
     * Resuelve el vehículo que se está reseñando (antes lo hacía el grafo de
     * navegación a partir de los mocks) y, en modo edición, precarga el
     * formulario. El backend no tiene GET /reviews/{id}, así que la reseña
     * se busca dentro de las del vehículo — la misma consulta que ya hace
     * la pantalla de detalle.
     */
    fun start(vehicleId: Int, reviewId: Int) {
        if (_uiState.value.car != null) return // ya cargado: no repetir en recomposiciones

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val vehicleResult = vehicleRepository.getVehicleById(vehicleId)
            val vehicle = vehicleResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar el vehículo"
                    )
                }
                return@launch
            }

            val isEditing = reviewId != ScreenRoute.NewReview.NO_REVIEW_ID
            val existing = if (isEditing) {
                reviewRepository.getReviewsByVehicle(vehicleId)
                    .getOrNull()
                    ?.firstOrNull { it.id == reviewId }
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    car = vehicle.toReviewCarSummary(),
                    reviewId = existing?.id,
                    rating = existing?.rating ?: 0,
                    experience = existing?.comment.orEmpty(),
                    errorMessage = if (isEditing && existing == null) {
                        "No se encontró la reseña que querías editar"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun updateRating(rating: Int) {
        _uiState.update { it.copy(rating = rating, errorMessage = null) }
    }

    fun updateExperience(input: String) {
        _uiState.update { it.copy(experience = input) }
    }

    fun toggleAspect(aspect: ReviewAspect) {
        _uiState.update { current ->
            val selectedAspects = if (aspect in current.selectedAspects) {
                current.selectedAspects - aspect
            } else {
                current.selectedAspects + aspect
            }
            current.copy(selectedAspects = selectedAspects)
        }
    }

    fun addPhotoButtonPress() {
        _uiState.update { current ->
            if (current.photoCount < 3) current.copy(photoCount = current.photoCount + 1) else current
        }
    }

    /**
     * Valida del lado del cliente antes de llamar al backend. El backend
     * acepta rating 0, pero las estrellas solo expresan 1..5 y una reseña
     * de 0 estrellas por descuido no tiene sentido.
     */
    fun save(vehicleId: Int) {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.rating !in 1..5) {
            _uiState.update { it.copy(errorMessage = "Selecciona una calificación de 1 a 5 estrellas") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val trimmed = current.experience.trim()
            val result = current.reviewId?.let { reviewId ->
                // Al editar se manda el texto tal cual, incluso vacío: si se
                // mandara null, Gson omitiría el campo y el backend (que solo
                // toca los campos presentes) dejaría el comentario anterior,
                // así que no habría forma de borrarlo.
                reviewRepository.updateReview(reviewId, current.rating, trimmed)
            } ?: reviewRepository.createReview(
                userId = CURRENT_USER_ID,
                vehicleId = vehicleId,
                rating = current.rating,
                // Al crear sí conviene null: deja la columna nula en vez de
                // guardar una cadena vacía.
                comment = trimmed.ifBlank { null }
            )

            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isSaving = false, isSaved = true) },
                    onFailure = { error ->
                        state.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "No se pudo guardar la reseña"
                        )
                    }
                )
            }
        }
    }
}
