package com.example.motormates.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.FeedTab
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.toFeedReviewUi
import com.example.motormates.data.model.toFeedVehicleUi
import com.example.motormates.data.model.toStoryUser
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.StoryRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.repository.news.NewsRepository
import com.example.motormates.data.seed.FirestoreSeeder
import com.example.motormates.data.util.FeedRanker
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.util.UserSuggester
import com.example.motormates.data.util.vehicleLabel
import com.example.motormates.ui.common.LikedReviewsState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val reviewRepository: ReviewRepository,
    private val followRepository: FollowRepository,
    private val authRepository: AuthRepository,
    private val likeRepository: LikeRepository,
    private val storyRepository: StoryRepository,
    private val firestoreSeeder: FirestoreSeeder,
    private val preferencesRepository: PreferencesRepository,
    private val timeSource: TimeSource,
    private val newsRepository: NewsRepository
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

    // ---- Personalización ----
    // Los vehículos sin ordenar y las reseñas recientes de todos: con eso y
    // las preferencias, FeedRanker arma el orden de Explorar, "Para ti", las
    // sugerencias y las reseñas destacadas de Siguiendo.
    private val catalog = MutableStateFlow<List<Vehicle>>(emptyList())
    private val recentReviews = MutableStateFlow<List<Review>>(emptyList())
    private val featuredReviews = MutableStateFlow<List<Review>>(emptyList())
    /** Seguidos desde las sugerencias: se ocultan ya, sin esperar al listener. */
    private val justFollowed = MutableStateFlow<Set<String>>(emptySet())
    private var personalizationJob: Job? = null

    init {
        observeStories()
        viewModelScope.launch {
            // Primera vez en una BD vacía: se queman los datos de prueba
            // antes de pedir los vehículos. Si falla (p. ej. reglas de
            // Firestore), igual se intenta cargar lo que haya.
            runCatching { firestoreSeeder.seedIfEmpty() }
            runCatching { firestoreSeeder.backfillVehicleOwners() }
            loadVehicles()
            loadNews()
        }
        observeFollowingReviews()
        observePersonalization()
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
                        catalog.value = vehicles
                        // Orden provisional (el del catálogo) hasta que llegue
                        // el ranking: así la lista no espera a las reseñas.
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            vehicles = current.vehicles.ifEmpty { vehicles.map { it.toFeedVehicleUi() } }
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

    /**
     * Las reseñas de los últimos días de todos los usuarios: dan la
     * popularidad, lo que pasa en la ciudad y a quién sugerir. Una sola
     * lectura por carga (no un listener): el orden del feed no necesita
     * moverse con cada reseña nueva.
     */
    private suspend fun loadRecentReviews() {
        val since = timeSource.nowMillis() - RANKING_WINDOW_MS
        withTimeoutOrNull(RANKING_TIMEOUT_MS) {
            reviewRepository.observeRecentReviews(since).first()
        }?.getOrNull()?.let { recentReviews.value = it }
    }

    private fun observePersonalization() {
        val currentUserId = authRepository.getCurrentUser()?.id
        personalizationJob?.cancel()
        personalizationJob = viewModelScope.launch {
            launch { loadRecentReviews() }

            val followingIds = currentUserId
                ?.let { followRepository.observeFollowingIds(it) }
                ?: flowOf(emptyList())

            combine(
                catalog,
                recentReviews,
                // Sin esperar a Firestore: arranca sin gustos y se reordena al llegar.
                preferencesRepository.observeMine().onStart { emit(UserPreferences()) },
                followingIds.onStart { emit(emptyList()) },
                justFollowed
            ) { vehicles, reviews, preferences, following, followedNow ->
                PersonalizationInput(vehicles, reviews, preferences, following.toSet() + followedNow)
            }.collect { input -> personalize(input, currentUserId) }
        }
    }

    private fun personalize(input: PersonalizationInput, currentUserId: String?) {
        val (vehicles, reviews, preferences, following) = input
        val now = timeSource.nowMillis()
        val ranked = FeedRanker.rankVehicles(vehicles, reviews, preferences, now)
        val forYou = FeedRanker.forYou(ranked, preferences, excludedOwnerId = currentUserId)
        val excludedAuthors = following + listOfNotNull(currentUserId)

        featuredReviews.value = FeedRanker
            .rankReviews(reviews, vehicles, preferences, now, excludedUserIds = excludedAuthors)
            .take(FEATURED_REVIEWS)

        _uiState.update { state ->
            state.copy(
                vehicles = if (vehicles.isEmpty()) state.vehicles else ranked.map { item ->
                    // En la lista solo se explica lo personal; "popular" sobra en cada tarjeta.
                    item.vehicle.toFeedVehicleUi()
                        .copy(reason = item.reason?.takeIf { item.isPersonal }?.vehicleLabel())
                },
                forYou = forYou.map { item ->
                    item.vehicle.toFeedVehicleUi().copy(reason = item.reason?.vehicleLabel())
                },
                hasPreferences = preferences.hasTaste,
                suggestions = UserSuggester.suggest(
                    reviews = reviews,
                    vehicles = vehicles,
                    preferences = preferences,
                    currentUserId = currentUserId,
                    followingIds = following,
                    limit = FEED_SUGGESTIONS
                )
            )
        }
    }

    /** Seguir desde las sugerencias del feed. */
    fun followSuggested(userId: String) {
        val currentUserId = authRepository.getCurrentUser()?.id ?: return
        justFollowed.update { it + userId }
        viewModelScope.launch {
            followRepository.followUser(currentUserId, userId).onFailure {
                justFollowed.update { it - userId }
            }
        }
    }

    private fun loadNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isNewsLoading = true, newsErrorMessage = null) }
            val result = newsRepository.getLatestNews()
            _uiState.update { current ->
                result.fold(
                    onSuccess = { articles ->
                        current.copy(
                            isNewsLoading = false,
                            newsArticles = articles
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isNewsLoading = false,
                            newsErrorMessage = error.message ?: "No se pudieron cargar las noticias"
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
                combine(reviewsFlow, featuredReviews) { result, featured ->
                    (result.getOrNull().orEmpty() + featured).map { it.id }.distinct()
                }
            )

            combine(
                reviewsFlow,
                likedFlow,
                vehicleImagesByVehicleId,
                featuredReviews
            ) { result, liked, images, featured -> FollowingSnapshot(result, liked, images, featured) }
                .collect { (result, liked, images, featured) ->
                    // Las destacadas se pintan con el mismo corazón en vivo.
                    val followingIds = result.getOrNull().orEmpty().map { it.id }.toSet()
                    _uiState.update { current ->
                        current.copy(
                            featuredReviews = featured
                                .filterNot { it.id in followingIds }
                                .map { it.toFeedReviewUi(images[it.vehicleId], liked) }
                        )
                    }
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
        val review = _uiState.value.followingReviews.find { it.id == reviewId }
            ?: _uiState.value.featuredReviews.find { it.id == reviewId }
            ?: return
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

    private data class FollowingSnapshot(
        val result: Result<List<Review>>,
        val liked: Set<String>,
        val images: Map<String, String?>,
        val featured: List<Review>
    )

    private data class PersonalizationInput(
        val vehicles: List<Vehicle>,
        val reviews: List<Review>,
        val preferences: UserPreferences,
        val following: Set<String>
    )

    private companion object {
        /** Ventana de reseñas para el ranking: popularidad y "en tu ciudad". */
        const val RANKING_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
        const val RANKING_TIMEOUT_MS = 8_000L
        const val FEATURED_REVIEWS = 10
        const val FEED_SUGGESTIONS = 5
    }
}
