package com.example.motormates.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.garageOf
import com.example.motormates.data.model.toActivityUi
import com.example.motormates.data.repository.ActivityRepository
import com.example.motormates.data.model.toUserProfile
import com.example.motormates.data.model.toUserReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.BuildRepository
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.util.TimeSource
import com.example.motormates.data.repository.ProfileImageRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.UserRepository
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
class UserViewModel @Inject constructor(
    private val profileImageRepository: ProfileImageRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val vehicleRepository: VehicleRepository,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val buildRepository: BuildRepository,
    private val eventRepository: EventRepository,
    private val timeSource: TimeSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    // Foto de cada vehículo reseñado. Es un flow para que entre al combine:
    // así no hay que releer los vehículos en cada emisión del perfil.
    private val vehicleImagesByVehicleId = MutableStateFlow<Map<String, String?>>(emptyMap())

    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            profileImageRepository.profileImageUrl.collect { photoUrl ->
                _uiState.update { it.copy(profileImageUrl = photoUrl) }
            }
        }

        load()
    }

    /**
     * El perfil y sus reseñas quedan escuchándose en Firestore: así el
     * contador de seguidores sube cuando alguien me sigue, sin recargar.
     */
    fun load() {
        val authUser = authRepository.getCurrentUser()
        if (authUser == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "No se encontró tu sesión, vuelve a iniciar sesión"
                )
            }
            return
        }

        loadVehicles(authUser.id)

        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                userRepository.observeUserProfile(authUser.id),
                reviewRepository.observeReviewsByUsers(listOf(authUser.id)),
                vehicleImagesByVehicleId
            ) { userResult, reviewsResult, images ->
                Triple(userResult, reviewsResult, images)
            }.collect { (userResult, reviewsResult, images) ->
                // Si fallan las reseñas el perfil se muestra igual, sin lista.
                val reviews = reviewsResult.getOrElse { emptyList() }

                _uiState.update { current ->
                    userResult.fold(
                        onSuccess = { user ->
                            current.copy(
                                isLoading = false,
                                errorMessage = null,
                                profile = user.toUserProfile(reviewsCount = reviews.size),
                                reviews = reviews.map { it.toUserReviewUi(images[it.vehicleId]) },
                                coverImageUrl = user.coverUrl
                            )
                        },
                        onFailure = { error ->
                            current.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "No se pudo cargar el perfil"
                            )
                        }
                    )
                }
            }
        }
    }

    /**
     * Una consulta de vehículos para dos cosas: las miniaturas de mis
     * reseñas y mi garaje (los que publiqué). Si falla, las reseñas se ven
     * igual (sin miniatura) y el garaje se queda como estaba.
     */
    private fun loadVehicles(uid: String) {
        viewModelScope.launch {
            vehicleRepository.getVehicles().onSuccess { vehicles ->
                vehicleImagesByVehicleId.value = vehicles.associate { it.id to it.imageUrl }
                _uiState.update { it.copy(cars = vehicles.garageOf(uid)) }
            }
        }
    }

    /**
     * Salida de emergencia: si el perfil no carga (p. ej. la cuenta quedó
     * sin documento en Firestore), desde aquí se puede cerrar sesión sin
     * tener que pasar por Editar perfil. Mismo patrón que EditProfileViewModel.
     */
    fun logOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        // El perfil es una pestaña que se conserva: si publiqué un carro
        // mientras tanto, al abrir el garaje se vuelve a consultar.
        if (tab == ProfileTab.GARAGE) {
            authRepository.getCurrentUser()?.id?.let(::loadVehicles)
        }
        if (tab == ProfileTab.BUILDS) loadBuilds()
        // La actividad se consulta cada vez que se abre la pestaña: así
        // aparece lo que hice desde la última vez sin tener que recargar.
        if (tab == ProfileTab.ACTIVITY) loadActivity()
        if (tab == ProfileTab.EVENTS) loadMyEvents()
    }

    /** Se consulta cada vez que se abre la pestaña: así sale lo que marqué hace poco. */
    fun loadMyEvents() {
        viewModelScope.launch {
            val now = timeSource.nowMillis()
            _uiState.update { it.copy(isEventsLoading = it.myEvents.isEmpty(), eventsError = null, nowMillis = now) }
            eventRepository.getMyEvents(now).fold(
                onSuccess = { events -> _uiState.update { it.copy(isEventsLoading = false, myEvents = events) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isEventsLoading = false, eventsError = error.message ?: "No se pudieron cargar tus eventos")
                    }
                }
            )
        }
    }

    fun loadActivity() {
        viewModelScope.launch {
            // El spinner solo la primera vez; después se refresca por debajo.
            _uiState.update { it.copy(isActivityLoading = it.activity.isEmpty(), activityError = null) }
            activityRepository.getMyActivity().fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(isActivityLoading = false, activity = items.map { item -> item.toActivityUi() })
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isActivityLoading = false, activityError = error.message ?: "No se pudo cargar tu actividad")
                    }
                }
            )
        }
    }

    /**
     * Se piden al abrir la pestaña (y cada vez que se vuelve a ella): así un
     * build recién guardado aparece sin tener que recargar todo el perfil.
     */
    fun loadBuilds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuildsLoading = true) }
            val builds = buildRepository.getMyBuilds().getOrElse { emptyList() }
            _uiState.update { it.copy(isBuildsLoading = false, builds = builds) }
        }
    }

    fun askDeleteReview(reviewId: String) {
        _uiState.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _uiState.update { it.copy(pendingDeleteReviewId = null) }
    }

    /** No hace falta recargar: el flow repinta la lista y el conteo. */
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
