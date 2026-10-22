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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun loadProfileByUid(uid: String) {
        profileUid = uid
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userResult = userRepository.getCurrentUserProfile(uid)
            val reviews = reviewRepository.getReviewsByUser(uid).getOrElse { emptyList() }
            // Foto de cada vehículo reseñado. Si falla, las reseñas se
            // muestran igual, sin miniatura.
            val vehicleImages = vehicleRepository.getVehicles()
                .getOrElse { emptyList() }
                .associate { it.id to it.imageUrl }
            val currentUserId = authRepository.getCurrentUser()?.id
            val isFollowing = currentUserId?.let { myId ->
                followRepository.isFollowing(myId, uid).getOrElse { false }
            } ?: false

            _uiState.update { current ->
                userResult.fold(
                    onSuccess = { user ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            profile = user.toUserProfile(reviewsCount = reviews.size),
                            reviews = reviews.map { it.toUserReviewUi(vehicleImages[it.vehicleId]) },
                            selectedTab = ProfileTab.REVIEWS,
                            avatarUrl = user.avatarUrl,
                            coverUrl = user.coverUrl,
                            isFollowedByMe = isFollowing
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

    /**
     * Optimista: cambia el botón y el contador de seguidores de una vez y
     * llama a Firestore después; si falla, revierte.
     */
    fun toggleFollow() {
        val targetUid = profileUid ?: return
        val myId = authRepository.getCurrentUser()?.id ?: return
        val wasFollowing = _uiState.value.isFollowedByMe
        val profile = _uiState.value.profile ?: return
        val previousFollowersCount = profile.followersDisplay.toIntOrNull() ?: 0

        _uiState.update {
            it.copy(
                isFollowedByMe = !wasFollowing,
                profile = profile.copy(
                    followersDisplay = "${(previousFollowersCount + if (wasFollowing) -1 else 1).coerceAtLeast(0)}"
                )
            )
        }

        viewModelScope.launch {
            val result = if (wasFollowing) {
                followRepository.unfollowUser(myId, targetUid)
            } else {
                followRepository.followUser(myId, targetUid)
            }

            result.onFailure {
                _uiState.update {
                    it.copy(
                        isFollowedByMe = wasFollowing,
                        profile = profile.copy(followersDisplay = "$previousFollowersCount")
                    )
                }
            }

            // Solo al seguir (no al dejar de seguir), y solo si sí se guardó.
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
