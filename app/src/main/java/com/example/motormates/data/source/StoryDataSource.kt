package com.example.motormates.data.source

import android.net.Uri
import com.example.motormates.data.dto.LikeDto
import com.example.motormates.data.dto.StoryFirestoreDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface StoryDataSource {
    suspend fun createStory(userId: String, userName: String, userAvatarUrl: String?, imageUri: Uri)
    suspend fun updateStory(userId: String, storyId: String, imageUri: Uri): String
    suspend fun deleteStory(userId: String, storyId: String)
    fun observeActiveStories(sinceMillis: Long): Flow<List<StoryFirestoreDto>>
    fun observeStory(storyId: String): Flow<StoryFirestoreDto?>
    suspend fun likeStory(storyId: String, userId: String)
    suspend fun unlikeStory(storyId: String, userId: String)
    fun observeIsLiked(storyId: String, userId: String): Flow<Boolean>
    fun observeLikerIds(storyId: String): Flow<List<String>>
}

class FirestoreStoryDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : StoryDataSource {

    private val stories get() = firestore.collection(STORIES_COLLECTION)

    override suspend fun createStory(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        imageUri: Uri
    ) {
        val storyId = UUID.randomUUID().toString()
        val imageRef = storage.reference.child("stories/$userId/$storyId.jpg")

        imageRef.putFile(imageUri).await()
        val imageUrl = imageRef.downloadUrl.await().toString()

        val story = StoryFirestoreDto(
            id = storyId,
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            imageUrl = imageUrl,
            createdAt = System.currentTimeMillis()
        )
        stories.document(storyId).set(story).await()
    }

    override suspend fun updateStory(userId: String, storyId: String, imageUri: Uri): String {
        val imageRef = storage.reference.child("stories/$userId/$storyId.jpg")
        imageRef.putFile(imageUri).await()
        val imageUrl = imageRef.downloadUrl.await().toString()

        stories.document(storyId).update(
            mapOf(
                "imageUrl" to imageUrl,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return imageUrl
    }

    override suspend fun deleteStory(userId: String, storyId: String) {
        stories.document(storyId).delete().await()
        runCatching { storage.reference.child("stories/$userId/$storyId.jpg").delete().await() }
    }

    override fun observeActiveStories(sinceMillis: Long): Flow<List<StoryFirestoreDto>> =
        observeQuery(stories.whereGreaterThan("createdAt", sinceMillis))

    override fun observeStory(storyId: String): Flow<StoryFirestoreDto?> = callbackFlow {
        val registration = stories.document(storyId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(StoryFirestoreDto::class.java))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun likeStory(storyId: String, userId: String) {
        val storyRef = stories.document(storyId)
        val likeRef = likeRef(storyId, userId)

        firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)

            if (!likeSnapshot.exists()) {
                transaction.set(likeRef, LikeDto(userId = userId, createdAt = System.currentTimeMillis()))
                transaction.update(storyRef, "likesCount", FieldValue.increment(1))
            }

            null
        }.await()
    }

    override suspend fun unlikeStory(storyId: String, userId: String) {
        val storyRef = stories.document(storyId)
        val likeRef = likeRef(storyId, userId)

        firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)

            if (likeSnapshot.exists()) {
                transaction.delete(likeRef)
                transaction.update(storyRef, "likesCount", FieldValue.increment(-1))
            }

            null
        }.await()
    }

    override fun observeIsLiked(storyId: String, userId: String): Flow<Boolean> = callbackFlow {
        val registration = likeRef(storyId, userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.exists() == true)
        }
        awaitClose { registration.remove() }
    }

    override fun observeLikerIds(storyId: String): Flow<List<String>> = callbackFlow {
        val registration = stories.document(storyId).collection(LIKES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.id }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    private fun likeRef(storyId: String, userId: String) =
        stories.document(storyId).collection(LIKES_COLLECTION).document(userId)

    private fun observeQuery(query: Query): Flow<List<StoryFirestoreDto>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val dtos = snapshot?.toObjects(StoryFirestoreDto::class.java).orEmpty()
            trySend(dtos.sortedByDescending { it.createdAt })
        }
        awaitClose { registration.remove() }
    }
}
