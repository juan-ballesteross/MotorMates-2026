package com.example.motormates.data.dto

import com.google.firebase.firestore.DocumentId

data class ChatFirestoreDto(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L,
    val lastReadAt: Map<String, Long> = emptyMap()
)

data class MessageFirestoreDto(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
