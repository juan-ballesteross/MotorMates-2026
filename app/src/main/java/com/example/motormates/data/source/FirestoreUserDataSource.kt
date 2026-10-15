package com.example.motormates.data.source

import com.example.motormates.data.dto.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"

class FirestoreUserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserDataSource {

    override suspend fun createUser(user: UserDto) {
        // El id del documento es el uid de Auth.
        firestore.collection(USERS_COLLECTION).document(user.id).set(user).await()
    }
}