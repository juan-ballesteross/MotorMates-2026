package com.example.motormates.ui.story

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.FeedMocks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class StoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState

    fun loadStory(index: Int) {
        val stories = FeedMocks.sampleStories
        val story = stories.getOrNull(index)?.takeIf { it.imageRes != null }
        _uiState.update { it.copy(story = story, isLiked = false, totalStories = stories.size) }
    }

    fun likeButtonPress() {
        _uiState.update { it.copy(isLiked = !it.isLiked) }
    }

    fun nextIndexWithContent(from: Int): Int? =
        (from until FeedMocks.sampleStories.size).firstOrNull { FeedMocks.sampleStories[it].imageRes != null }

    fun previousIndexWithContent(from: Int): Int? =
        (from downTo 0).firstOrNull { FeedMocks.sampleStories[it].imageRes != null }
}
