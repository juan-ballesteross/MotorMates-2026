package com.example.motormates.ui.editProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                username = UserMocks.sampleUserProfile.name,
                bio = UserMocks.sampleUserProfile.bio,
                cars = UserMocks.sampleUserCars
            )
        }
    }

    fun updateUsername(input: String) {
        _uiState.update { it.copy(username = input) }
    }

    fun updateBio(input: String) {
        _uiState.update { it.copy(bio = input) }
    }

    fun logOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }
}
