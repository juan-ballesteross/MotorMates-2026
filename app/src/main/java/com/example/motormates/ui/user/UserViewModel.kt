package com.example.motormates.ui.user

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.UserMocks
import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class UserUiState(
    val profile: UserProfile = UserMocks.sampleUserProfile,
    val cars: List<GarageCar> = UserMocks.sampleUserCars,
    val selectedTab: ProfileTab = ProfileTab.REVIEWS
)

class UserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
