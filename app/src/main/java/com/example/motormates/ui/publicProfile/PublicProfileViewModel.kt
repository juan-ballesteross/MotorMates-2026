package com.example.motormates.ui.publicProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.toUserProfile
import com.example.motormates.data.model.toUserReviewUi
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.UserRepository
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
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState

    fun loadProfileByUid(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userResult = userRepository.getCurrentUserProfile(uid)
            val reviews = reviewRepository.getReviewsByUser(uid).getOrElse { emptyList() }

            _uiState.update { current ->
                userResult.fold(
                    onSuccess = { user ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            profile = user.toUserProfile(reviewsCount = reviews.size),
                            reviews = reviews.map { it.toUserReviewUi() },
                            selectedTab = ProfileTab.REVIEWS,
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

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
