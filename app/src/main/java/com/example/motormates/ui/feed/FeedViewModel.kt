package com.example.motormates.ui.feed

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.FeedMocks
import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FeedUiState(
    val stories: List<StoryUser> = FeedMocks.sampleStories,
    val post: ReviewPost = FeedMocks.samplePost
)

class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState
}
