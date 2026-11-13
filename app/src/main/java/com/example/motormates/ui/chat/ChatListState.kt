package com.example.motormates.ui.chat

import com.example.motormates.data.model.ChatPreview

data class ChatListUiState(
    val allChats: List<ChatPreview> = emptyList(),
    val chats: List<ChatPreview> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
