package com.example.motormates.data.source

import com.example.motormates.data.model.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthDataSource {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user).toAuthUser()
    }

    override suspend fun signUp(fullName: String, email: String, password: String): AuthUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = requireNotNull(result.user)
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()

        user.updateProfile(profileUpdates).await()
        return user.toAuthUser(displayNameOverride = fullName)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): AuthUser? = firebaseAuth.currentUser?.toAuthUser()
}

private fun FirebaseUser.toAuthUser(displayNameOverride: String? = null): AuthUser =
    AuthUser(
        id = uid,
        email = email,
        displayName = displayNameOverride ?: displayName,
        photoUrl = photoUrl?.toString()
    )
