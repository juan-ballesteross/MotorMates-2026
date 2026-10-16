package com.example.motormates.data.mock

import com.example.motormates.R
import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser
import com.example.motormates.data.seed.SeedData

object FeedMocks {
    val sampleStories = listOf(
        StoryUser(userId = SeedData.SEED_USER_RODRIGO, name = "Rodrigo", avatarResId = R.drawable.user_3, imageRes = R.drawable.rodrigo_feed, caption = "Antes de una carrerita"),
        StoryUser(userId = SeedData.SEED_USER_SOFIA, name = "Sofía", avatarResId = R.drawable.user1, imageRes = R.drawable.sofia_feed, caption = "Posuda"),
        StoryUser(userId = SeedData.SEED_USER_IVAN, name = "Iván", avatarResId = R.drawable.user2)
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
