package com.example.motormates.ui.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.toUserProfile
import com.example.motormates.data.model.toUserReviewUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.NotificationRepository
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

/**
 * Perfil de otro usuario registrado en Firestore (colección "users"). Se
 * llega desde el buscador o tocando el autor de una reseña.
 */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val vehicleRepository: VehicleRepository,
    private val followRepository: FollowRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState

    private var profileUid: String? = null

    // Mismo criterio que en UserViewModel: entra al combine para no releer
    // los vehículos en cada emisión del perfil.
    private val vehicleImagesByVehicleId = MutableStateFlow<Map<String, String?>>(emptyMap())

    private var profileJob: Job? = null

    /**
     * El perfil y sus reseñas quedan escuchándose: si publica una reseña o
     * alguien más lo sigue, lo que está en pantalla se actualiza solo.
     */
    fun loadProfileByUid(uid: String) {
        profileUid = uid

        loadVehicleImages()
        loadIsFollowing(uid)

        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                userRepository.observeUserProfile(uid),
                reviewRepository.observeReviewsByUsers(listOf(uid)),
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
                                avatarUrl = user.avatarUrl,
                                coverUrl = user.coverUrl
                            )
                        },
                        onFailure = { error ->
                            current.copy(
                                isLoading = false,
                                profile = null,
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
     * El estado del botón se consulta una vez; después lo mueve toggleFollow.
     * No necesita listener: solo cambia cuando el propio usuario lo toca.
     */
    private fun loadIsFollowing(uid: String) {
        val currentUserId = authRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            val isFollowing = followRepository.isFollowing(currentUserId, uid).getOrElse { false }
            _uiState.update { it.copy(isFollowedByMe = isFollowing) }
        }
    }

    /**
     * Optimista solo con el botón: el contador de seguidores ya no se toca a
     * mano, llega del snapshot del perfil. Si falla, se revierte el botón.
     */
    fun toggleFollow() {
        val targetUid = profileUid ?: return
        val myId = authRepository.getCurrentUser()?.id ?: return
        val wasFollowing = _uiState.value.isFollowedByMe

        _uiState.update { it.copy(isFollowedByMe = !wasFollowing) }

        viewModelScope.launch {
            val result = if (wasFollowing) {
                followRepository.unfollowUser(myId, targetUid)
            } else {
                followRepository.followUser(myId, targetUid)
            }

            result.onFailure {
                _uiState.update { it.copy(isFollowedByMe = wasFollowing) }
            }

            if (!wasFollowing && result.isSuccess) {
                notificationRepository.notify(
                    recipientUid = targetUid,
                    type = NotificationType.FOLLOW,
                    actorId = myId
                )
            }
        }
    }

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
