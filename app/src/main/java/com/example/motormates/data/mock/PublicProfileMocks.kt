package com.example.motormates.data.mock

import com.example.motormates.R
import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.UserProfile

object PublicProfileMocks {
    private const val SOFIA_STORY_INDEX = 4

    val sofiaAvatarResId = R.drawable.user1

    private val sofiaProfile = UserProfile(
        name = "Sofia Reyes",
        handle = "@Xxs0fi_Pr0Dr1verxX",
        location = "Bogotá",
        bio = "Amante a la velocidad y a las carreras, si me quieres desafiar, solo escribeme >:v.",
        reviewsCount = 68,
        followersDisplay = "3.2K",
        followingDisplay = "1.5"
    )

    private val sofiaCars = listOf(
        GarageCar("Honda NSX", R.drawable.honda_nsx, year = 2005, categoryLabel = "Super Deportivo")
    )

    fun avatarForStoryIndex(storyIndex: Int): Int? =
        if (storyIndex == SOFIA_STORY_INDEX) sofiaAvatarResId else null

    fun profileForStoryIndex(storyIndex: Int): UserProfile? =
        if (storyIndex == SOFIA_STORY_INDEX) sofiaProfile else null

    fun carsForStoryIndex(storyIndex: Int): List<GarageCar> =
        if (storyIndex == SOFIA_STORY_INDEX) sofiaCars else emptyList()
}
