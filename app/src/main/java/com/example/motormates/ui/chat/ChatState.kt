package com.example.motormates.ui.chat

import com.example.motormates.data.model.ChatMessage

data class ChatUiState(
    val chatId: String? = null,
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserAvatarUrl: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
