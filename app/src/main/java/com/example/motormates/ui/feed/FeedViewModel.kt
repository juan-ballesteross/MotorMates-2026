package com.example.motormates.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.FeedTab
import com.example.motormates.data.model.toFeedReviewUi
import com.example.motormates.data.model.toFeedVehicleUi
import com.example.motormates.data.model.toStoryUser
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.StoryRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.seed.FirestoreSeeder
import com.example.motormates.ui.common.LikedReviewsState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val followRepository: FollowRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository,
    private val storyRepository: StoryRepository,
    private val firestoreSeeder: FirestoreSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    // Mismo criterio que en VehicleDetailViewModel: el "ya di like" se
    // escucha aparte, en tiempo real. Cuando el usuario sigue a alguien más
    // la lista crece y la escucha se reabre con las reseñas nuevas.
    private val likedReviews = LikedReviewsState(likeRepository)

    // La foto del carro no viaja dentro de la reseña: se resuelve con los
    // vehículos ya cargados. Es un flow para que, cuando terminen de cargar,
    // las reseñas se vuelvan a pintar con su miniatura.
    private val vehicleImagesByVehicleId = MutableStateFlow<Map<String, String?>>(emptyMap())

    private var followingJob: Job? = null
    private var storiesJob: Job? = null

    init {
        observeStories()
        viewModelScope.launch {
            // Primera vez en una BD vacía: se queman los datos de prueba
            // antes de pedir los vehículos. Si falla (p. ej. reglas de
            // Firestore), igual se intenta cargar lo que haya.
            runCatching { firestoreSeeder.seedIfEmpty() }
            loadVehicles()
        }
        observeFollowingReviews()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeStories() {
        val currentUserId = authRepository.getCurrentUser()?.id ?: return

        storiesJob?.cancel()
        storiesJob = viewModelScope.launch {
            followRepository.observeFollowingIds(currentUserId)
                .flatMapLatest { followingIds ->
                    val visibleUserIds = followingIds.toSet() + currentUserId
                    storyRepository.observeActiveStories()
                        .map { stories -> stories.filter { it.userId in visibleUserIds } }
                }
                .collect { stories ->
                    _uiState.update { it.copy(stories = stories.map { story -> story.toStoryUser() }) }
                }
        }
    }

    fun postStory(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingStory = true, storyErrorMessage = null) }
            val result = storyRepository.postStory(imageUri)
            _uiState.update {
                it.copy(
                    isPostingStory = false,
                    storyErrorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun dismissStoryError() {
        _uiState.update { it.copy(storyErrorMessage = null) }
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

            // Compartido: lo leen la lista y la escucha de likes.
            val reviewsFlow = followRepository.observeFollowingIds(currentUserId)
                .flatMapLatest { ids -> reviewRepository.observeReviewsByUsers(ids) }
                .shareIn(this, SharingStarted.Eagerly, replay = 1)
            val likedFlow = likedReviews.observe(
                currentUserId,
                reviewsFlow.map { result -> result.getOrNull().orEmpty().map { it.id } }
            )

            combine(
                reviewsFlow,
                likedFlow,
                vehicleImagesByVehicleId
            ) { result, liked, images -> Triple(result, liked, images) }
                .collect { (result, liked, images) ->
                    result.fold(
                        onSuccess = { reviews ->
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

    /** Optimista solo con el corazón: el contador llega con la reseña. */
    fun toggleLike(reviewId: String) {
        val userId = authRepository.getCurrentUser()?.id ?: return
        val review = _uiState.value.followingReviews.find { it.id == reviewId } ?: return
        val wasLiked = review.isLikedByMe

        likedReviews.setPending(reviewId, !wasLiked)

        viewModelScope.launch {
            val result = if (wasLiked) {
                likeRepository.unlikeReview(reviewId, userId)
            } else {
                likeRepository.likeReview(reviewId, userId, review.userId, review.vehicleId)
            }

            result.onFailure { likedReviews.discardPending(reviewId) }
        }
    }
}
