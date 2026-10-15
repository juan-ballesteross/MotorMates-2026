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
    private val profileImageDataSource: ProfileImageDataSource,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ProfileImageRepository {

    override val profileImageUrl: StateFlow<String?> = profileImageDataSource.profileImageUrl

    override suspend fun uploadProfileImage(uri: Uri): Result<String> {
        return try {
            val downloadUrl = profileImageDataSource.uploadProfileImage(uri)
            // Se guarda también en Firestore (campo best-effort: si falla,
            // la foto ya quedó subida y puesta en tu propio perfil, así que
            // no se revierte nada, solo no se ve todavía en perfiles ajenos).
            authRepository.getCurrentUser()?.id?.let { uid ->
                userRepository.updateCurrentUserAvatar(uid, downloadUrl)
            }
            Result.success(downloadUrl)
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
