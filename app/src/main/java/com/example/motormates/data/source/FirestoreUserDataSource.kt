package com.example.motormates.data.source

import com.example.motormates.data.dto.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /**
     * Mismo molde que authState en FirebaseAuthDataSource. Gracias a esto el
     * contador de seguidores del perfil se mueve solo: lo escribe la
     * transacción de follow desde el otro lado.
     */
    override fun observeUser(uid: String): Flow<UserDto?> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Un documento que no está en el caché local llega como un
                // primer evento con exists=false e isFromCache=true, antes de
                // que responda el servidor: eso no es "no existe", es que
                // todavía no se sabe.
                if (snapshot != null && !snapshot.exists() && snapshot.metadata.isFromCache) {
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserDto::class.java))
            }
        awaitClose { registration.remove() }
    }
}