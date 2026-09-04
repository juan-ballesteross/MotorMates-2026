package com.example.motormates.ui.editProfile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class EditProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState

    fun updateUsername(input: String) {
        _uiState.update { it.copy(username = input) }
    }

    fun updateBio(input: String) {
        _uiState.update { it.copy(bio = input) }
    }
}
