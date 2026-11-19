package com.example.motormates.data.model

data class ChatPreview(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserAvatarUrl: String?,
    val lastMessageText: String,
    val lastMessageSenderId: String,
    val lastMessageAt: String?,
    val timeAgo: String = "",
    val hasUnread: Boolean = false
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: String?,
    val isMine: Boolean,
    val imageUrl: String? = null,
    /** No null cuando el mensaje es una respuesta a una historia. */
    val storyReply: StoryReplyRef? = null
)

/** La historia a la que responde un mensaje. */
data class StoryReplyRef(
    val storyId: String,
    val imageUrl: String?
)
