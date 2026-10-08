package com.example.motormates.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lista completa de reseñas de un vehículo. Pinta los mismos ReviewUi que
 * el detalle, con el mismo componente, en vez de un modelo de comentario
 * aparte: así las estrellas y los botones de editar/eliminar salen gratis.
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState

    fun load(vehicleId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = reviewRepository.getReviewsByVehicle(vehicleId)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { reviews ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            reviews = reviews.map { it.toReviewUi() }
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar las reseñas"
                        )
                    }
                )
            }
        }
    }

    fun askDeleteReview(reviewId: Int) {
        _uiState.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _uiState.update { it.copy(pendingDeleteReviewId = null) }
    }

    fun confirmDeleteReview(vehicleId: Int) {
        val reviewId = _uiState.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeleteReviewId = null) }

            reviewRepository.deleteReview(reviewId).fold(
                onSuccess = { load(vehicleId) },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "No se pudo eliminar la reseña")
                    }
                }
            )
        }
    }
}
