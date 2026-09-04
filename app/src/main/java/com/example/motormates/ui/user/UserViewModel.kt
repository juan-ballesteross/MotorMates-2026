package com.example.motormates.ui.user

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.ProfileTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class UserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                profile = UserMocks.sampleUserProfile,
                cars = UserMocks.sampleUserCars
            )
        }
    }

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
