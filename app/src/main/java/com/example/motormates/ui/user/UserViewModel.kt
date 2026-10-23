package com.example.motormates.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.toUserProfile
import com.example.motormates.data.model.toUserReviewUi
import com.example.motormates.data.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    // Foto de cada vehículo reseñado. Es un flow para que entre al combine:
    // así no hay que releer los vehículos en cada emisión del perfil.
    private val vehicleImagesByVehicleId = MutableStateFlow<Map<String, String?>>(emptyMap())

    private var profileJob: Job? = null

    init {
        // El garaje se queda con datos locales: el backend no lo modela.
        _uiState.update { it.copy(cars = UserMocks.sampleUserCars) }

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

        loadVehicleImages()

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

    /** Si falla, las reseñas se muestran igual, sin miniatura. */
    private fun loadVehicleImages() {
        viewModelScope.launch {
            vehicleImagesByVehicleId.value = vehicleRepository.getVehicles()
                .getOrElse { emptyList() }
                .associate { it.id to it.imageUrl }
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
