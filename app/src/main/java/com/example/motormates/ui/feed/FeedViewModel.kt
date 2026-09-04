package com.example.motormates.ui.feed

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.FeedMocks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                stories = FeedMocks.sampleStories,
                posts = FeedMocks.samplePosts
            )
        }
    }
}
