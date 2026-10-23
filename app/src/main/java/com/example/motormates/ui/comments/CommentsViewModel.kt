package com.example.motormates.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.NotificationRepository
import com.example.motormates.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    // Igual que en VehicleDetailViewModel: se guarda para avisarle al autor
    // de la reseña a qué carro le dieron like.
    private var currentVehicleId: String? = null

    // Mismo criterio que en VehicleDetailViewModel: el "ya di like" no viene
    // en el snapshot de la reseña, se consulta una vez y lo mueve toggleLike.
    private val likedReviewIds = MutableStateFlow<Set<String>>(emptySet())
    private var likedIdsLoaded = false

    private var reviewsJob: Job? = null

    /** Las reseñas quedan escuchándose: llegan y se actualizan solas. */
    fun load(vehicleId: String) {
        currentVehicleId = vehicleId
        likedIdsLoaded = false
        likedReviewIds.value = emptySet()
        reviewsJob?.cancel()

        val currentUserId = authRepository.getCurrentUser()?.id

        reviewsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                reviewRepository.observeReviewsByVehicle(vehicleId),
                likedReviewIds
            ) { result, liked -> result to liked }
                .collect { (result, liked) ->
                    result.fold(
                        onSuccess = { reviews ->
                            if (currentUserId != null && !likedIdsLoaded) {
                                likedIdsLoaded = true
                                loadLikedIds(currentUserId, reviews.map { it.id })
                            }

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

    private fun loadLikedIds(userId: String, reviewIds: List<String>) {
        viewModelScope.launch {
            likedReviewIds.value = likeRepository.getLikedReviewIds(userId, reviewIds)
                .getOrElse { emptySet() }
        }
    }

    /** Optimista solo con el corazón: el contador llega del snapshot. */
    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _uiState.value.reviews.find { it.id == reviewId } ?: return
        val wasLiked = review.isLikedByMe

        setLiked(reviewId, !wasLiked)

        viewModelScope.launch {
            val result = if (wasLiked) {
                likeRepository.unlikeReview(reviewId, userId)
            } else {
                likeRepository.likeReview(reviewId, userId)
            }

            result.onFailure { setLiked(reviewId, wasLiked) }

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

    private fun setLiked(reviewId: String, liked: Boolean) {
        likedReviewIds.update { if (liked) it + reviewId else it - reviewId }
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
