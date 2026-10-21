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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
     * El perfil (nombre, handle, ubicación, bio) sale de Firestore, del
     * usuario real que inició sesión con Firebase Auth, y sus reseñas son
     * las de la colección "reviews" cuyo userId es su uid.
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val authUser = authRepository.getCurrentUser()
            if (authUser == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se encontró tu sesión, vuelve a iniciar sesión"
                    )
                }
                return@launch
            }

            val userResult = userRepository.getCurrentUserProfile(authUser.id)
            val reviews = reviewRepository.getReviewsByUser(authUser.id)
                .getOrElse { emptyList() }
            // Foto de cada vehículo reseñado. Si falla, las reseñas se
            // muestran igual, sin miniatura.
            val vehicleImages = vehicleRepository.getVehicles()
                .getOrElse { emptyList() }
                .associate { it.id to it.imageUrl }

            _uiState.update { current ->
                userResult.fold(
                    onSuccess = { user ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            profile = user.toUserProfile(reviewsCount = reviews.size),
                            reviews = reviews.map { it.toUserReviewUi(vehicleImages[it.vehicleId]) },
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

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun askDeleteReview(reviewId: String) {
        _uiState.update { it.copy(pendingDeleteReviewId = reviewId) }
    }

    fun dismissDeleteReview() {
        _uiState.update { it.copy(pendingDeleteReviewId = null) }
    }

    fun confirmDeleteReview() {
        val reviewId = _uiState.value.pendingDeleteReviewId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeleteReviewId = null) }

            reviewRepository.deleteReview(reviewId).fold(
                onSuccess = { load() },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "No se pudo eliminar la reseña")
                    }
                }
            )
        }
    }
}
