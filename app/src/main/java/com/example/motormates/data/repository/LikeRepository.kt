package com.example.motormates.data.repository

import com.example.motormates.data.source.LikeDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject

interface LikeRepository {
    suspend fun likeReview(reviewId: String, userId: String): Result<Unit>
}

class LikeRepositoryImpl @Inject constructor(
    private val likeDataSource: LikeDataSource
) : LikeRepository {

    override suspend fun likeReview(reviewId: String, userId: String): Result<Unit> {
        return try {
            likeDataSource.likeReview(reviewId, userId)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Sin conexión a internet, verifica tu red"
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "No tienes permiso para dar like"
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Este review ya no existe"
                else -> "No se pudo dar like, intenta de nuevo"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }
}