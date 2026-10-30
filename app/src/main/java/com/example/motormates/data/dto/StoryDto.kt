package com.example.motormates.data.dto

import com.example.motormates.data.model.Story
import com.google.firebase.firestore.DocumentId
import java.time.Instant

data class StoryFirestoreDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val likesCount: Long = 0L
) {
    fun toDomain(): Story = Story(
        id = id,
        userId = userId,
        userName = userName,
        userAvatarUrl = userAvatarUrl,
        imageUrl = imageUrl,
        createdAt = if (createdAt > 0) Instant.ofEpochMilli(createdAt).toString() else null,
        likesCount = likesCount
    )
}
