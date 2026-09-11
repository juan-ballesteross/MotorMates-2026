package com.example.motormates.ui.publicProfile

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.PublicProfileMocks
import com.example.motormates.data.model.ProfileTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PublicProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState

    fun loadProfile(storyIndex: Int) {
        _uiState.update {
            it.copy(
                avatarResId = PublicProfileMocks.avatarForStoryIndex(storyIndex),
                profile = PublicProfileMocks.profileForStoryIndex(storyIndex),
                cars = PublicProfileMocks.carsForStoryIndex(storyIndex),
                selectedTab = ProfileTab.REVIEWS
            )
        }
    }

    fun updateSelectedTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
