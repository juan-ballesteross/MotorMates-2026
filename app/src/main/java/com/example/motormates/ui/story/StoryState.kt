package com.example.motormates.ui.story

import com.example.motormates.data.model.StoryUser
import com.example.motormates.data.model.UserSearchResult

data class StoryUiState(
    val story: StoryUser? = null,
    val isLiked: Boolean = false,
    val likesCount: Long = 0L,
    val totalStories: Int = 0,
    val isOwnStory: Boolean = false,
    val pendingDelete: Boolean = false,
    val isUpdating: Boolean = false,
    val showLikers: Boolean = false,
    val isLoadingLikers: Boolean = false,
    val likers: List<UserSearchResult> = emptyList(),
    val errorMessage: String? = null
)
