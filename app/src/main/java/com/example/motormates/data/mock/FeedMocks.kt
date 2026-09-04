package com.example.motormates.data.mock

import com.example.motormates.R
import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser

object FeedMocks {
    val sampleStories = listOf(
        StoryUser("Marco", R.drawable.user1),
        StoryUser("Elena", R.drawable.user2),
        StoryUser("Diego", R.drawable.user_3),
        StoryUser("Sofía", R.drawable.user1)
    )

    val samplePost = ReviewPost(
        userName = "Marco Ferretti",
        avatarResId = R.drawable.user1,
        carName = "Porsche 911 GT3",
        imageResId = R.drawable.porsche_gt3_rs,
        timeAgo = "2h",
        rating = 5,
        caption = "Primera vuelta con el GT3 en Ascari. La dirección es de otro planeta.",
        likes = 284,
        comments = 32,
        shares = 9
    )

    val samplePosts = listOf(samplePost)
}
