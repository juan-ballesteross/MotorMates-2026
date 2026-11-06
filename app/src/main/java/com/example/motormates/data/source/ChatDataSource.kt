package com.example.motormates.data.source

import com.example.motormates.data.dto.ChatFirestoreDto
import com.example.motormates.data.dto.MessageFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface ChatDataSource {
    suspend fun getOrCreateChat(uidA: String, uidB: String): String
    fun observeChats(uid: String): Flow<List<ChatFirestoreDto>>
    fun observeMessages(chatId: String): Flow<List<MessageFirestoreDto>>
    suspend fun sendMessage(chatId: String, senderId: String, text: String)
    suspend fun markAsRead(chatId: String, uid: String)
}

class FirestoreChatDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatDataSource {

    private fun chatIdFor(uidA: String, uidB: String) =
        listOf(uidA, uidB).sorted().joinToString("_")

    private fun chatRef(chatId: String) = firestore.collection(CHATS_COLLECTION).document(chatId)

    override suspend fun getOrCreateChat(uidA: String, uidB: String): String {
        val chatId = chatIdFor(uidA, uidB)
        val ref = chatRef(chatId)
        if (!ref.get().await().exists()) {
            ref.set(
                ChatFirestoreDto(
                    id = chatId,
                    participants = listOf(uidA, uidB)
                )
            ).await()
        }
        return chatId
    }

    override fun observeChats(uid: String): Flow<List<ChatFirestoreDto>> = callbackFlow {
        val registration = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(ChatFirestoreDto::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    override fun observeMessages(chatId: String): Flow<List<MessageFirestoreDto>> = callbackFlow {
        val registration = chatRef(chatId).collection(MESSAGES_COLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(MessageFirestoreDto::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(chatId: String, senderId: String, text: String) {
        val now = System.currentTimeMillis()
        val messageRef = chatRef(chatId).collection(MESSAGES_COLLECTION).document()

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                MessageFirestoreDto(id = messageRef.id, senderId = senderId, text = text, createdAt = now)
            )
            batch.update(
                chatRef(chatId),
                mapOf(
                    "lastMessageText" to text,
                    "lastMessageSenderId" to senderId,
                    "lastMessageAt" to now
                )
            )
        }.await()
    }

    override suspend fun markAsRead(chatId: String, uid: String) {
        chatRef(chatId).update(mapOf("lastReadAt.$uid" to System.currentTimeMillis())).await()
    }
}
