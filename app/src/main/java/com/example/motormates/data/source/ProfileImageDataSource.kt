package com.example.motormates.data.source

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

interface ProfileImageDataSource {
    val profileImageUrl: StateFlow<String?>

    suspend fun uploadProfileImage(uri: Uri): String
}

@Singleton
class FirebaseProfileImageDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : ProfileImageDataSource {

    private val _profileImageUrl = MutableStateFlow(firebaseAuth.currentUser?.photoUrl?.toString())
    override val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _profileImageUrl.value = auth.currentUser?.photoUrl?.toString()
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override suspend fun uploadProfileImage(uri: Uri): String {
        val user = requireNotNull(firebaseAuth.currentUser) {
            "Debes iniciar sesion para cambiar tu foto"
        }
        val imageRef = firebaseStorage.reference.child("profile_images/${user.uid}/avatar")

        imageRef.putFile(uri).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()
        val changes = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(downloadUrl))
            .build()

        user.updateProfile(changes).await()
        _profileImageUrl.value = downloadUrl
        return downloadUrl
    }
}
