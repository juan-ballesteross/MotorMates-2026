package com.example.motormates.data.source

import com.example.motormates.data.dto.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await


class FirestoreUserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserDataSource {

    override suspend fun createUser(user: UserDto) {
        // El id del documento es el uid de Auth.
        firestore.collection(USERS_COLLECTION).document(user.id).set(user).await()
    }

    override suspend fun getUser(uid: String): UserDto? {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return snapshot.toObject(UserDto::class.java)
    }

    override suspend fun updateUser(uid: String, fullName: String, bio: String) {
        firestore.collection(USERS_COLLECTION).document(uid)
            .update(mapOf("fullName" to fullName, "bio" to bio))
            .await()
    }

    override suspend fun updateAvatarUrl(uid: String, avatarUrl: String) {
        firestore.collection(USERS_COLLECTION).document(uid)
            .update("avatarUrl", avatarUrl)
            .await()
    }

    override suspend fun updateCoverUrl(uid: String, coverUrl: String) {
        firestore.collection(USERS_COLLECTION).document(uid)
            .update("coverUrl", coverUrl)
            .await()
    }

    override suspend fun getAllUsers(): List<UserDto> {
        val snapshot = firestore.collection(USERS_COLLECTION).get().await()
        return snapshot.toObjects(UserDto::class.java)
    }
}