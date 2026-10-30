package com.example.motormates.data.repository

import android.net.Uri
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.Story
import com.example.motormates.data.model.User
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.StoryDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface StoryRepository {
    suspend fun postStory(imageUri: Uri): Result<Unit>
    suspend fun updateStory(storyId: String, imageUri: Uri): Result<String>
    suspend fun deleteStory(storyId: String): Result<Unit>
    fun observeActiveStories(): Flow<List<Story>>
    suspend fun likeStory(storyId: String, storyOwnerId: String): Result<Unit>
    suspend fun unlikeStory(storyId: String): Result<Unit>
    fun observeIsLiked(storyId: String): Flow<Boolean>
    fun observeLikesCount(storyId: String): Flow<Long>
    fun observeLikers(storyId: String): Flow<List<User>>
}

class StoryRepositoryImpl @Inject constructor(
    private val storyDataSource: StoryDataSource,
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource,
    private val notificationRepository: NotificationRepository
) : StoryRepository {

    override suspend fun postStory(imageUri: Uri): Result<Unit> {
        val currentUser = authDataSource.getCurrentUser()
            ?: return Result.failure(Exception("Debes iniciar sesión para publicar una historia"))

        return try {
            val profile = userDataSource.getUser(currentUser.id)
            storyDataSource.createStory(
                userId = currentUser.id,
                userName = profile?.fullName?.ifBlank { null } ?: currentUser.displayName ?: "Usuario",
                userAvatarUrl = profile?.avatarUrl ?: currentUser.photoUrl,
                imageUri = imageUri
            )
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo publicar la historia"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo publicar la historia"))
        }
    }

    override suspend fun updateStory(storyId: String, imageUri: Uri): Result<String> {
        val userId = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("Debes iniciar sesión para editar una historia"))

        return try {
            Result.success(storyDataSource.updateStory(userId, storyId, imageUri))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo actualizar la historia"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo actualizar la historia"))
        }
    }

    override suspend fun deleteStory(storyId: String): Result<Unit> {
        val userId = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("Debes iniciar sesión para eliminar una historia"))

        return try {
            storyDataSource.deleteStory(userId, storyId)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo eliminar la historia"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo eliminar la historia"))
        }
    }

    override fun observeActiveStories(): Flow<List<Story>> {
        val sinceMillis = System.currentTimeMillis() - ACTIVE_WINDOW_MS
        return storyDataSource.observeActiveStories(sinceMillis)
            .map { dtos -> dtos.map { it.toDomain() }.groupedByUser() }
            .catch { emit(emptyList()) }
    }

    private fun List<Story>.groupedByUser(): List<Story> {
        val byUser = groupBy { it.userId }
        return byUser.keys
            .sortedByDescending { uid -> byUser.getValue(uid).maxOf { it.createdAt.orEmpty() } }
            .flatMap { uid -> byUser.getValue(uid).sortedBy { it.createdAt.orEmpty() } }
    }

    override suspend fun likeStory(storyId: String, storyOwnerId: String): Result<Unit> {
        val userId = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("Debes iniciar sesión para dar like"))

        return try {
            storyDataSource.likeStory(storyId, userId)
            notificationRepository.notify(
                recipientUid = storyOwnerId,
                type = NotificationType.LIKE_STORY,
                actorId = userId
            )
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo dar like a la historia"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo dar like a la historia"))
        }
    }

    override suspend fun unlikeStory(storyId: String): Result<Unit> {
        val userId = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("Debes iniciar sesión para quitar el like"))

        return try {
            storyDataSource.unlikeStory(storyId, userId)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception("No se pudo quitar el like"))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión a internet, verifica tu red"))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo quitar el like"))
        }
    }

    override fun observeIsLiked(storyId: String): Flow<Boolean> {
        val userId = authDataSource.getCurrentUser()?.id ?: return flowOf(false)
        return storyDataSource.observeIsLiked(storyId, userId).catch { emit(false) }
    }

    override fun observeLikesCount(storyId: String): Flow<Long> =
        storyDataSource.observeStory(storyId)
            .map { it?.likesCount ?: 0L }
            .catch { emit(0L) }

    override fun observeLikers(storyId: String): Flow<List<User>> =
        storyDataSource.observeLikerIds(storyId)
            .map { uids -> resolveUsers(uids) }
            .catch { emit(emptyList()) }

    private suspend fun resolveUsers(uids: List<String>): List<User> = coroutineScope {
        uids
            .map { uid -> async { userDataSource.getUser(uid) } }
            .mapNotNull { it.await()?.toDomain() }
    }

    private companion object {
        const val ACTIVE_WINDOW_MS = 24 * 60 * 60 * 1000L
    }
}
