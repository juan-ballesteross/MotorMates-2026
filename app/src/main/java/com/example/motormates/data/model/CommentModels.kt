package com.example.motormates.data.model

import com.example.motormates.R

data class CommentUi(
    val id: String,
    val authorName: String,
    val avatarResId: Int = 0,
    val timeAgo: String,
    val text: String,
    val isLiked: Boolean = false
)

// ===== Datos locales quemados =====

val mockComments = listOf(
    CommentUi(
        id = "1",
        authorName = "Ivan Perez",
        timeAgo = "3h",
        text = "Primera vuelta con el GT3 en Ascari. La dirección es de otro planeta.",
        avatarResId = R.drawable.user2
    ),
    CommentUi(
        id = "2",
        authorName = "Sofia Reyes",
        timeAgo = "50 min",
        text = "Y que tal te parecio, cuentameeeee",
        avatarResId = R.drawable.user1
    )
)

const val mockCommentsTotalCount = 128