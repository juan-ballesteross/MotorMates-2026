package com.example.motormates.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.FeedMocks
import com.example.motormates.data.model.FeedTab
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.toFeedReviewUi
import com.example.motormates.data.model.toFeedVehicleUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.NotificationRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.seed.FirestoreSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val followRepository: FollowRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository,
    private val notificationRepository: NotificationRepository,
    private val firestoreSeeder: FirestoreSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    // Mismo criterio que en VehicleDetailViewModel: el "ya di like" no viene
    // en el snapshot de la reseña, se consulta y después lo mueve toggleLike.
    // A diferencia del detalle, acá la lista crece cuando el usuario sigue a
    // alguien más, así que se consulta solo por las reseñas nuevas.
    private val likedReviewIds = MutableStateFlow<Set<String>>(emptySet())
    private var checkedReviewIds = emptySet<String>()

    // La foto del carro no viaja dentro de la reseña: se resuelve con los
    // vehículos ya cargados. Es un flow para que, cuando terminen de cargar,
    // las reseñas se vuelvan a pintar con su miniatura.
    private val vehicleImagesByVehicleId = MutableStateFlow<Map<String, String?>>(emptyMap())

    private var followingJob: Job? = null

    init {
        // Las historias siguen siendo datos locales, el backend no las modela.
        _uiState.update { it.copy(stories = FeedMocks.sampleStories) }
        viewModelScope.launch {
            // Primera vez en una BD vacía: se queman los datos de prueba
            // antes de pedir los vehículos. Si falla (p. ej. reglas de
            // Firestore), igual se intenta cargar lo que haya.
            runCatching { firestoreSeeder.seedIfEmpty() }
            loadVehicles()
        }
        observeFollowingReviews()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = vehicleRepository.getVehicles()
            _uiState.update { current ->
                result.fold(
                    onSuccess = { vehicles ->
                        vehicleImagesByVehicleId.value = vehicles.associate { it.id to it.imageUrl }
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            vehicles = vehicles.map { it.toFeedVehicleUi() }
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar los vehículos"
                        )
                    }
                )
            }
        }
    }

    fun updateSelectedTab(tab: FeedTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * El "Reintentar" de la pestaña Siguiendo: vuelve a abrir los listeners.
     * Es su propia acción y no loadVehicles, porque las dos pestañas se
     * alimentan de fuentes distintas.
     */
    fun retryFollowingReviews() {
        observeFollowingReviews()
    }

    /**
     * Dos listeners en cadena: primero a quién sigo, y con esos uid las
     * reseñas que publican. flatMapLatest bota la escucha anterior cuando
     * la lista de seguidos cambia, así seguir o dejar de seguir a alguien
     * rearma el feed sin recargar la pantalla.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFollowingReviews() {
        val currentUserId = authRepository.getCurrentUser()?.id ?: return

        followingJob?.cancel()
        followingJob = viewModelScope.launch {
            _uiState.update { it.copy(isFollowingLoading = true, followingErrorMessage = null) }

            combine(
                followRepository.observeFollowingIds(currentUserId)
                    .flatMapLatest { ids -> reviewRepository.observeReviewsByUsers(ids) },
                likedReviewIds,
                vehicleImagesByVehicleId
            ) { result, liked, images -> Triple(result, liked, images) }
                .collect { (result, liked, images) ->
                    result.fold(
                        onSuccess = { reviews ->
                            loadLikedIdsFor(currentUserId, reviews.map { it.id })

                            _uiState.update { current ->
                                current.copy(
                                    isFollowingLoading = false,
                                    followingErrorMessage = null,
                                    followingReviews = reviews.map { review ->
                                        review.toFeedReviewUi(images[review.vehicleId], liked)
                                    }
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isFollowingLoading = false,
                                    followingErrorMessage = error.message
                                        ?: "No se pudieron cargar las reseñas"
                                )
                            }
                        }
                    )
                }
        }
    }

    /** Solo pregunta por las reseñas que todavía no se han revisado. */
    private fun loadLikedIdsFor(userId: String, reviewIds: List<String>) {
        val pending = reviewIds - checkedReviewIds
        if (pending.isEmpty()) return
        checkedReviewIds = checkedReviewIds + pending

        viewModelScope.launch {
            val liked = likeRepository.getLikedReviewIds(userId, pending)
                .getOrElse { emptySet() }
            if (liked.isNotEmpty()) {
                likedReviewIds.update { it + liked }
            }
        }
    }

    /** Optimista solo con el corazón: el contador llega del snapshot. */
    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _uiState.value.followingReviews.find { it.id == reviewId } ?: return
        val wasLiked = review.isLikedByMe

        setLiked(reviewId, !wasLiked)

        viewModelScope.launch {
            val result = if (wasLiked) {
                likeRepository.unlikeReview(reviewId, userId)
            } else {
                likeRepository.likeReview(reviewId, userId)
            }

            result.onFailure { setLiked(reviewId, wasLiked) }

            // Solo al dar like (no al quitarlo), y solo si sí se guardó.
            if (!wasLiked && result.isSuccess) {
                notificationRepository.notify(
                    recipientUid = review.userId,
                    type = NotificationType.LIKE,
                    actorId = userId,
                    vehicleId = review.vehicleId,
                    reviewId = reviewId
                )
            }
        }
    }

    private fun setLiked(reviewId: String, liked: Boolean) {
        likedReviewIds.update { if (liked) it + reviewId else it - reviewId }
    }
}
