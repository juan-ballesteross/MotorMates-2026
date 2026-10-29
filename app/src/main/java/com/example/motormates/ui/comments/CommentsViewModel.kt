package com.example.motormates.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.ui.common.LikedReviewsState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lista completa de reseñas de un vehículo. Pinta los mismos ReviewUi que
 * el detalle, con el mismo componente, en vez de un modelo de comentario
 * aparte: así las estrellas y los botones de editar/eliminar salen gratis.
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState

    private var currentVehicleId: String? = null

    // Mismo criterio que en VehicleDetailViewModel: el "ya di like" se
    // escucha aparte, en tiempo real, y se combina con las reseñas.
    private val likedReviews = LikedReviewsState(likeRepository)

    private var reviewsJob: Job? = null

    /** Las reseñas quedan escuchándose: llegan y se actualizan solas. */
    fun load(vehicleId: String) {
        currentVehicleId = vehicleId
        reviewsJob?.cancel()

        val currentUserId = authRepository.getCurrentUser()?.id

        reviewsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val reviewsFlow = reviewRepository.observeReviewsByVehicle(vehicleId)
                .shareIn(this, SharingStarted.Eagerly, replay = 1)
            val likedFlow = likedReviews.observe(
                currentUserId,
                reviewsFlow.map { result -> result.getOrNull().orEmpty().map { it.id } }
            )

            combine(reviewsFlow, likedFlow) { result, liked -> result to liked }
                .collect { (result, liked) ->
                    result.fold(
                        onSuccess = { reviews ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    reviews = reviews.map { review ->
                                        review.toReviewUi(currentUserId, liked)
                                    }
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "No se pudieron cargar las reseñas"
                                )
                            }
                        }
                    )
                }
        }
    }

    /** Optimista solo con el corazón: el contador llega con la reseña. */
    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _uiState.value.reviews.find { it.id == reviewId } ?: return
        val wasLiked = review.isLikedByMe

        likedReviews.setPending(reviewId, !wasLiked)

        viewModelScope.launch {
            val result = if (wasLiked) {
                likeRepository.unlikeReview(reviewId, userId)
            } else {
                likeRepository.likeReview(reviewId, userId, review.userId, currentVehicleId)
            }

            result.onFailure { likedReviews.discardPending(reviewId) }
        }
    }

    fun askDeleteReview(reviewId: String) {
        _uiState.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _uiState.update { it.copy(pendingDeleteReviewId = null) }
    }

    /** No hace falta recargar: el flow repinta la lista. */
    fun confirmDeleteReview() {
        val reviewId = _uiState.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeleteReviewId = null) }

            reviewRepository.deleteReview(reviewId).onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "No se pudo eliminar la reseña")
                }
            }
        }
    }
}
