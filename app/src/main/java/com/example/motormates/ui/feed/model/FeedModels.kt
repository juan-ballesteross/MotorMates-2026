package com.example.motormates.ui.feed.model

data class StoryUser(val name: String)

data class ReviewPost(
    val userName: String,
    val carName: String,
    val timeAgo: String,
    val rating: Int,
    val caption: String,
    val likes: Int,
    val comments: Int,
    val shares: Int
)

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
