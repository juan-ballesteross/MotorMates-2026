package com.example.motormates.ui.feed

import com.example.motormates.data.mock.FeedMocks
import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser

data class FeedUiState(
    val stories: List<StoryUser> = FeedMocks.sampleStories,
    val posts: List<ReviewPost> = FeedMocks.samplePosts
)
