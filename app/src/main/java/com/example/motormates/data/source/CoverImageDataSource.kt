package com.example.motormates.data.source

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

interface CoverImageDataSource {
    suspend fun uploadCoverImage(uri: Uri): String
}

@Singleton
class FirebaseCoverImageDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : CoverImageDataSource {

    override suspend fun uploadCoverImage(uri: Uri): String {
        val user = requireNotNull(firebaseAuth.currentUser) {
            "Debes iniciar sesion para cambiar tu foto de portada"
        }
        val imageRef = firebaseStorage.reference.child("profile_covers/${user.uid}/cover")

        imageRef.putFile(uri).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()
        return "$downloadUrl&cb=${System.currentTimeMillis()}"
    }
}
