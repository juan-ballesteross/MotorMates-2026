package com.example.motormates.data.mock

import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser

object FeedMocks {
    val sampleStories = listOf("Marco", "Elena", "Diego", "Sofía").map { StoryUser(it) }

    val samplePost = ReviewPost(
        userName = "Marco Ferretti",
        carName = "Porsche 911 GT3",
        timeAgo = "2h",
        rating = 5,
        caption = "Primera vuelta con el GT3 en Ascari. La dirección es de otro planeta.",
        likes = 284,
        comments = 32,
        shares = 9
    )
}
