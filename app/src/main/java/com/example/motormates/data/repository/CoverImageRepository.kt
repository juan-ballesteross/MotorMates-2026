package com.example.motormates.data.repository

import android.net.Uri
import com.example.motormates.data.source.CoverImageDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.storage.StorageException
import javax.inject.Inject

interface CoverImageRepository {
    suspend fun uploadCoverImage(uri: Uri): Result<String>
}

class CoverImageRepositoryImpl @Inject constructor(
    private val coverImageDataSource: CoverImageDataSource,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : CoverImageRepository {

    override suspend fun uploadCoverImage(uri: Uri): Result<String> {
        return try {
            val downloadUrl = coverImageDataSource.uploadCoverImage(uri)
            // Igual que con el avatar: si falla el guardado en Firestore,
            // la imagen ya quedó subida, así que no se revierte nada.
            authRepository.getCurrentUser()?.id?.let { uid ->
                userRepository.updateCurrentUserCover(uid, downloadUrl)
            }
            Result.success(downloadUrl)
        } catch (e: IllegalStateException) {
            Result.failure(Exception(e.message ?: "Debes iniciar sesion para cambiar tu foto de portada"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Sin conexion a internet, verifica tu red"))
        } catch (e: StorageException) {
            Result.failure(Exception("No se pudo subir la foto de portada"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al actualizar la foto de portada"))
        }
    }
}
