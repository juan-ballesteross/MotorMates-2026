package com.example.motormates.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.CURRENT_USER_ID
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.toUserProfile
import com.example.motormates.data.model.toUserReviewUi
import com.example.motormates.data.repository.ProfileImageRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.UserRepository
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
    private val reviewRepository: ReviewRepository
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

    /** Perfil y reseñas del usuario quemado (ver CURRENT_USER_ID). */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userResult = userRepository.getUserById(CURRENT_USER_ID)
            val reviews = reviewRepository.getReviewsByUser(CURRENT_USER_ID)
                .getOrElse { emptyList() }

            _uiState.update { current ->
                userResult.fold(
                    onSuccess = { user ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            profile = user.toUserProfile(reviewsCount = reviews.size),
                            reviews = reviews.map { it.toUserReviewUi() }
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
}
