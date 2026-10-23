package com.example.motormates.ui.vehicleDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.ReviewUi
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.NotificationRepository
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
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleDetailState())
    val state: StateFlow<VehicleDetailState> = _state

    private var currentVehicleId: String? = null

    /**
     * Carga vehículo y reseñas en la misma corrutina: la calificación
     * promedio y el conteo que muestra la cabecera se calculan a partir de
     * las reseñas, así que no tiene sentido pintar uno sin el otro.
     */
    fun load(vehicleId: String) {
        currentVehicleId = vehicleId
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
            val currentUserId = authRepository.getCurrentUser()?.id
            val likedReviewIds = currentUserId?.let { uid ->
                likeRepository.getLikedReviewIds(uid, reviews.map { it.id }).getOrElse { emptySet() }
            } ?: emptySet()

            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    vehicle = vehicle.toCarDetailUi(reviews),
                    reviews = reviews.map { review -> review.toReviewUi(currentUserId, likedReviewIds) }
                )
            }
        }
    }

    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _state.value.reviews.find { it.id == reviewId } ?: return
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
        _state.update { current ->
            current.copy(
                reviews = current.reviews.map { if (it.id == reviewId) transform(it) else it }
            )
        }
    }

    fun askDeleteReview(reviewId: String) {
        _state.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _state.update { it.copy(pendingDeleteReviewId = null) }
    }

    fun confirmDeleteReview(vehicleId: String) {
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
