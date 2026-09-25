package com.example.motormates.data.repository

import android.net.Uri
import com.example.motormates.data.source.ProfileImageDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.storage.StorageException
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

interface ProfileImageRepository {
    val profileImageUrl: StateFlow<String?>

    suspend fun uploadProfileImage(uri: Uri): Result<String>
}

class ProfileImageRepositoryImpl @Inject constructor(
    private val profileImageDataSource: ProfileImageDataSource
) : ProfileImageRepository {

    override val profileImageUrl: StateFlow<String?> = profileImageDataSource.profileImageUrl

    override suspend fun uploadProfileImage(uri: Uri): Result<String> {
        return try {
            Result.success(profileImageDataSource.uploadProfileImage(uri))
        } catch (e: IllegalStateException) {
            Result.failure(Exception(e.message ?: "Debes iniciar sesion para cambiar tu foto"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Sin conexion a internet, verifica tu red"))
        } catch (e: StorageException) {
            Result.failure(Exception("No se pudo subir la foto de perfil"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al actualizar la foto de perfil"))
        }
    }
}
