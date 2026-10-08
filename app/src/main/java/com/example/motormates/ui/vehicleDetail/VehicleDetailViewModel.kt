package com.example.motormates.ui.vehicleDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleDetailState())
    val state: StateFlow<VehicleDetailState> = _state

    /**
     * Carga vehículo y reseñas en la misma corrutina: la calificación
     * promedio y el conteo que muestra la cabecera se calculan a partir de
     * las reseñas, así que no tiene sentido pintar uno sin el otro.
     */
    fun load(vehicleId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val vehicleResult = vehicleRepository.getVehicleById(vehicleId)
            val vehicle = vehicleResult.getOrElse { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        vehicle = null,
                        errorMessage = error.message ?: "No se pudo cargar el vehículo"
                    )
                }
                return@launch
            }

            val reviews = reviewRepository.getReviewsByVehicle(vehicleId).getOrElse { emptyList() }

            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    vehicle = vehicle.toCarDetailUi(reviews),
                    reviews = reviews.map { review -> review.toReviewUi() }
                )
            }
        }
    }

    fun askDeleteReview(reviewId: Int) {
        _state.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _state.update { it.copy(pendingDeleteReviewId = null) }
    }

    fun confirmDeleteReview(vehicleId: Int) {
        val reviewId = _state.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _state.update { it.copy(pendingDeleteReviewId = null) }

            reviewRepository.deleteReview(reviewId).fold(
                // Se recarga todo para que el promedio y el conteo queden al día.
                onSuccess = { load(vehicleId) },
                onFailure = { error ->
                    _state.update {
                        it.copy(errorMessage = error.message ?: "No se pudo eliminar la reseña")
                    }
                }
            )
        }
    }

    /** Solo visual: el backend no tiene concepto de "guardados". */
    fun bookmarkButtonPress() {
        _state.update { it.copy(isBookmarked = !it.isBookmarked) }
    }
}
