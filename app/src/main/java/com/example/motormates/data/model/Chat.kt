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
    val isMine: Boolean
)
