package com.example.motormates.ui.story

import com.example.motormates.data.model.StoryUser

data class StoryUiState(
    val story: StoryUser? = null,
    val isLiked: Boolean = false,
    val totalStories: Int = 0
)
