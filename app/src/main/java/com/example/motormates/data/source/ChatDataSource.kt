package com.example.motormates.data.source

import android.net.Uri
import com.example.motormates.data.model.StoryReplyRef
import com.example.motormates.data.dto.ChatFirestoreDto
import com.example.motormates.data.dto.MessageFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface ChatDataSource {
    suspend fun getOrCreateChat(uidA: String, uidB: String): String
    fun observeChats(uid: String): Flow<List<ChatFirestoreDto>>
    fun observeMessages(chatId: String): Flow<List<MessageFirestoreDto>>
    /** storyReply: si el mensaje es una respuesta a una historia. */
    suspend fun sendMessage(chatId: String, senderId: String, text: String, storyReply: StoryReplyRef? = null, imageUrl: String? = null)
    suspend fun uploadChatImage(chatId: String, imageUri: Uri): String
    suspend fun markAsRead(chatId: String, uid: String)
}

class FirestoreChatDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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

    override suspend fun sendMessage(chatId: String, senderId: String, text: String, storyReply: StoryReplyRef?, imageUrl: String?) {
        val now = System.currentTimeMillis()
        val messageRef = chatRef(chatId).collection(MESSAGES_COLLECTION).document()

        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                MessageFirestoreDto(
                    id = messageRef.id,
                    senderId = senderId,
                    text = text,
                    createdAt = now,
                    imageUrl = imageUrl,
                    storyId = storyReply?.storyId,
                    storyImageUrl = storyReply?.imageUrl
                )
            )
            batch.update(
                chatRef(chatId),
                mapOf(
                    // En la lista de chats se nota que fue a una historia.
                    "lastMessageText" to when {
                        storyReply != null -> "Respondió a una historia: $text"
                        imageUrl != null && text.isBlank() -> "\uD83D\uDCF7 Foto"
                        else -> text
                    },
                    "lastMessageSenderId" to senderId,
                    "lastMessageAt" to now
                )
            )
        }.await()
    }

    override suspend fun uploadChatImage(chatId: String, imageUri: Uri): String {
        val imageRef = storage.reference.child("chats/$chatId/${UUID.randomUUID()}.jpg")
        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    override suspend fun markAsRead(chatId: String, uid: String) {
        chatRef(chatId).update(mapOf("lastReadAt.$uid" to System.currentTimeMillis())).await()
    }
}
