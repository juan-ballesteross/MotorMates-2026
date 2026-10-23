package com.example.motormates.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.ReviewUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.NotificationRepository
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
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState

    private var currentVehicleId: String? = null

    fun load(vehicleId: String) {
        currentVehicleId = vehicleId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = reviewRepository.getReviewsByVehicle(vehicleId)
            val currentUserId = authRepository.getCurrentUser()?.id
            val likedReviewIds = if (currentUserId != null) {
                val reviews = result.getOrElse { emptyList() }
                likeRepository.getLikedReviewIds(currentUserId, reviews.map { it.id }).getOrElse { emptySet() }
            } else {
                emptySet()
            }

            _uiState.update { current ->
                result.fold(
                    onSuccess = { reviews ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            reviews = reviews.map { it.toReviewUi(currentUserId, likedReviewIds) }
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

    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _uiState.value.reviews.find { it.id == reviewId } ?: return
        val wasLiked = review.isLikedByMe

        updateReview(reviewId) {
            it.copy(
                isLikedByMe = !wasLiked,
                likesCount = (it.likesCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
            )
        }

        viewModelScope.launch {
            val result = if (wasLiked) {
                likeRepository.unlikeReview(reviewId, userId)
            } else {
                likeRepository.likeReview(reviewId, userId)
            }

            result.onFailure {
                updateReview(reviewId) {
                    it.copy(
                        isLikedByMe = wasLiked,
                        likesCount = (it.likesCount + if (wasLiked) 1 else -1).coerceAtLeast(0)
                    )
                }
            }

            if (!wasLiked && result.isSuccess) {
                notificationRepository.notify(
                    recipientUid = review.userId,
                    type = NotificationType.LIKE,
                    actorId = userId,
                    vehicleId = currentVehicleId,
                    reviewId = reviewId
                )
            }
        }
    }

    private fun updateReview(reviewId: String, transform: (ReviewUi) -> ReviewUi) {
        _uiState.update { current ->
            current.copy(
                reviews = current.reviews.map { if (it.id == reviewId) transform(it) else it }
            )
        }
    }

    fun askDeleteReview(reviewId: String) {
        _uiState.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _uiState.update { it.copy(pendingDeleteReviewId = null) }
    }

    fun confirmDeleteReview(vehicleId: String) {
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
