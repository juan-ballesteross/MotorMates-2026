package com.example.motormates.data.repository

import com.example.motormates.data.dto.ChatFirestoreDto
import com.example.motormates.data.model.ChatMessage
import com.example.motormates.data.model.ChatPreview
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.ChatDataSource
import com.example.motormates.data.source.UserDataSource
import com.example.motormates.data.util.toRelativeTimeEs
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface ChatRepository {
    suspend fun openChatWith(otherUid: String): Result<String>
    fun observeChats(): Flow<Result<List<ChatPreview>>>
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(chatId: String, otherUserId: String, text: String): Result<Unit>
    suspend fun markChatAsRead(chatId: String)
    fun observeHasUnreadChats(): Flow<Boolean>
}

class ChatRepositoryImpl @Inject constructor(
    private val chatDataSource: ChatDataSource,
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource,
    private val notificationRepository: NotificationRepository
) : ChatRepository {

    override suspend fun openChatWith(otherUid: String): Result<String> {
        val currentUserId = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("Debes iniciar sesión para enviar mensajes"))
        return try {
            Result.success(chatDataSource.getOrCreateChat(currentUserId, otherUid))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo abrir el chat"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo abrir el chat"))
        }
    }

    override fun observeChats(): Flow<Result<List<ChatPreview>>> {
        val currentUserId = authDataSource.getCurrentUser()?.id
            ?: return flowOf(Result.failure(Exception("Debes iniciar sesión para ver tus mensajes")))

        return chatDataSource.observeChats(currentUserId)
            .map { dtos -> Result.success(dtos.toChatPreviews(currentUserId)) }
            .catch { emit(Result.failure(Exception("No se pudieron cargar tus mensajes"))) }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        val currentUserId = authDataSource.getCurrentUser()?.id
        return chatDataSource.observeMessages(chatId)
            .map { dtos ->
                dtos.sortedBy { it.createdAt }.map { dto ->
                    ChatMessage(
                        id = dto.id,
                        senderId = dto.senderId,
                        text = dto.text,
                        createdAt = if (dto.createdAt > 0) Instant.ofEpochMilli(dto.createdAt).toString() else null,
                        isMine = dto.senderId == currentUserId
                    )
                }
            }
            .catch { emit(emptyList()) }
    }

    override suspend fun markChatAsRead(chatId: String) {
        val currentUserId = authDataSource.getCurrentUser()?.id ?: return
        runCatching { chatDataSource.markAsRead(chatId, currentUserId) }
    }

    override fun observeHasUnreadChats(): Flow<Boolean> =
        observeChats()
            .map { result -> result.getOrNull()?.any { it.hasUnread } ?: false }
            .catch { emit(false) }

    override suspend fun sendMessage(chatId: String, otherUserId: String, text: String): Result<Unit> {
        val currentUserId = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("Debes iniciar sesión para enviar mensajes"))
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("El mensaje no puede estar vacío"))

        return try {
            chatDataSource.sendMessage(chatId, currentUserId, trimmed)
            notificationRepository.notify(
                recipientUid = otherUserId,
                type = NotificationType.MESSAGE,
                actorId = currentUserId
            )
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo enviar el mensaje"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo enviar el mensaje"))
        }
    }

    private suspend fun List<ChatFirestoreDto>.toChatPreviews(currentUserId: String): List<ChatPreview> =
        coroutineScope {
            sortedByDescending { it.lastMessageAt }
                .map { dto ->
                    async {
                        val otherUid = dto.participants.firstOrNull { it != currentUserId }.orEmpty()
                        val otherUser = runCatching { userDataSource.getUser(otherUid) }.getOrNull()
                        val lastMessageAtIso = if (dto.lastMessageAt > 0) {
                            Instant.ofEpochMilli(dto.lastMessageAt).toString()
                        } else {
                            null
                        }

                        val lastReadAt = dto.lastReadAt[currentUserId] ?: 0L
                        val hasUnread = dto.lastMessageSenderId.isNotBlank() &&
                            dto.lastMessageSenderId != currentUserId &&
                            dto.lastMessageAt > lastReadAt

                        ChatPreview(
                            chatId = dto.id,
                            otherUserId = otherUid,
                            otherUserName = otherUser?.fullName?.ifBlank { null } ?: "Usuario",
                            otherUserAvatarUrl = otherUser?.avatarUrl,
                            lastMessageText = dto.lastMessageText,
                            lastMessageSenderId = dto.lastMessageSenderId,
                            lastMessageAt = lastMessageAtIso,
                            timeAgo = lastMessageAtIso.toRelativeTimeEs(),
                            hasUnread = hasUnread
                        )
                    }
                }
                .map { it.await() }
        }
}
