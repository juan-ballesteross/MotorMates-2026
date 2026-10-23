package com.example.motormates.ui.vehicleDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
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

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleDetailState())
    val state: StateFlow<VehicleDetailState> = _state

    // Se guarda para poder avisarle al autor de la reseña qué carro era, sin
    // tener que pasarlo de nuevo como parámetro en cada toggleLike.
    private var currentVehicleId: String? = null

    // El vehículo se consulta una sola vez, pero la cabecera (promedio y
    // número de reseñas) se recalcula con cada emisión del flow de reseñas.
    private var currentVehicle: Vehicle? = null

    /**
     * Qué reseñas tengo likeadas, en tiempo real. No viene en la reseña (el
     * like vive aparte), así que se escucha por separado y se combina.
     */
    private val likedReviews = LikedReviewsState(likeRepository)

    private var reviewsJob: Job? = null

    /**
     * El vehículo se pide una vez y las reseñas quedan escuchándose en
     * tiempo real: si alguien publica, edita, borra o le da like a una
     * reseña de este carro, la pantalla se actualiza sola.
     */
    fun load(vehicleId: String) {
        currentVehicleId = vehicleId
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
            // Compartido: lo leen la lista y la escucha de likes, con un
            // solo listener detrás.
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

    /**
     * Optimista solo con el corazón: el contador no se toca a mano, llega
     * con la reseña. Si la escritura falla, el corazón vuelve a como estaba.
     */
    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _state.value.reviews.find { it.id == reviewId } ?: return
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
