package com.example.motormates.data.source

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Foto de un vehículo publicado. Siempre va a Firebase Storage, también con
 * el backend Express (ver DataBackend: Auth y las imágenes siguen en Firebase).
 */
interface VehicleImageDataSource {
    /** Sube la foto y devuelve su URL pública. */
    suspend fun uploadVehicleImage(ownerUid: String, uri: Uri): String
}

class FirebaseVehicleImageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) : VehicleImageDataSource {

    override suspend fun uploadVehicleImage(ownerUid: String, uri: Uri): String {
        val imageRef = storage.reference.child("vehicles/$ownerUid/${UUID.randomUUID()}.jpg")
        imageRef.putFile(uri).await()
        return imageRef.downloadUrl.await().toString()
    }
}
