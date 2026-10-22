package com.example.motormates.data.repository

import com.example.motormates.data.source.LikeDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject

interface LikeRepository {
    suspend fun likeReview(reviewId: String, userId: String): Result<Unit>
    suspend fun unlikeReview(reviewId: String, userId: String): Result<Unit>
    suspend fun getLikedReviewIds(userId: String, reviewIds: List<String>): Result<Set<String>>
}

class LikeRepositoryImpl @Inject constructor(
    private val likeDataSource: LikeDataSource
) : LikeRepository {

    override suspend fun likeReview(reviewId: String, userId: String): Result<Unit> {
        return try {
            likeDataSource.likeReview(reviewId, userId)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toLikeMessage()))
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }

    override suspend fun unlikeReview(reviewId: String, userId: String): Result<Unit> {
        return try {
            likeDataSource.unlikeReview(reviewId, userId)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toLikeMessage()))
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }

    override suspend fun getLikedReviewIds(userId: String, reviewIds: List<String>): Result<Set<String>> {
        return try {
            Result.success(likeDataSource.getLikedReviewIds(userId, reviewIds))
        } catch (e: Exception) {
            // No bloquea la pantalla: si falla, las reseñas se ven sin like marcado.
            Result.success(emptySet())
        }
    }

    private fun FirebaseFirestoreException.toLikeMessage(): String = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sin conexión a internet, verifica tu red"
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para dar like"
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "Este review ya no existe"
        else -> "No se pudo dar like, intenta de nuevo"
    }
}