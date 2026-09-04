package com.example.motormates.ui.feed

import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser

data class FeedUiState(
    val stories: List<StoryUser> = emptyList(),
    val posts: List<ReviewPost> = emptyList()
)
