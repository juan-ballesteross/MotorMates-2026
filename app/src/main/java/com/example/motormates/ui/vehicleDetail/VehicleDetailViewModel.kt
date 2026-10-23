package com.example.motormates.ui.vehicleDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.NotificationRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    // El vehículo se consulta una sola vez, pero la cabecera (promedio y
    // número de reseñas) se recalcula con cada emisión del flow de reseñas.
    private var currentVehicle: Vehicle? = null

    /**
     * Qué reseñas tengo likeadas. No viene en el snapshot de la reseña (el
     * like vive en una subcolección), así que se consulta una vez y después
     * lo mueve toggleLike. Al ser un flow, cambiarlo repinta los corazones.
     */
    private val likedReviewIds = MutableStateFlow<Set<String>>(emptySet())
    private var likedIdsLoaded = false

    private var reviewsJob: Job? = null

    /**
     * El vehículo se pide una vez y las reseñas quedan escuchándose en
     * tiempo real: si alguien publica, edita, borra o le da like a una
     * reseña de este carro, la pantalla se actualiza sola.
     */
    fun load(vehicleId: String) {
        currentVehicleId = vehicleId
        likedIdsLoaded = false
        likedReviewIds.value = emptySet()
        reviewsJob?.cancel()

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
            currentVehicle = vehicle

            observeReviews(vehicleId)
        }
    }

    private fun observeReviews(vehicleId: String) {
        val currentUserId = authRepository.getCurrentUser()?.id

        reviewsJob = viewModelScope.launch {
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

                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = null,
                                    vehicle = currentVehicle?.toCarDetailUi(reviews),
                                    reviews = reviews.map { review ->
                                        review.toReviewUi(currentUserId, liked)
                                    }
                                )
                            }
                        },
                        onFailure = { error ->
                            _state.update {
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

    /**
     * Optimista solo con el corazón: el contador ya no se toca a mano,
     * llega del snapshot de Firestore. Si la escritura falla, se devuelve
     * el corazón a como estaba.
     */
    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _state.value.reviews.find { it.id == reviewId } ?: return
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
        _state.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _state.update { it.copy(pendingDeleteReviewId = null) }
    }

    /** No hace falta recargar: el flow repinta la lista y la cabecera. */
    fun confirmDeleteReview() {
        val reviewId = _state.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _state.update { it.copy(pendingDeleteReviewId = null) }

            reviewRepository.deleteReview(reviewId).onFailure { error ->
                _state.update {
                    it.copy(errorMessage = error.message ?: "No se pudo eliminar la reseña")
                }
            }
        }
    }

    /** Solo visual: el backend no tiene concepto de "guardados". */
    fun bookmarkButtonPress() {
        _state.update { it.copy(isBookmarked = !it.isBookmarked) }
    }
}
