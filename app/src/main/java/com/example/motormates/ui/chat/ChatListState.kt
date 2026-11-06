package com.example.motormates.ui.chat

import com.example.motormates.data.model.ChatPreview

data class ChatListUiState(
    val chats: List<ChatPreview> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
