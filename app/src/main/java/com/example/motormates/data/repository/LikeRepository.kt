package com.example.motormates.data.repository

import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.source.LikeDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import retrofit2.HttpException

interface LikeRepository {
    /**
     * reviewAuthorId y vehicleId son para avisarle al autor de la reseña
     * (y decirle de qué carro era) cuando el like se guarda.
     */
    suspend fun likeReview(
        reviewId: String,
        userId: String,
        reviewAuthorId: String,
        vehicleId: String?
    ): Result<Unit>

    suspend fun unlikeReview(reviewId: String, userId: String): Result<Unit>

    /** A cuáles de estas reseñas les dio like el usuario, en tiempo real. */
    fun observeLikedReviewIds(userId: String, reviewIds: List<String>): Flow<Set<String>>
}

/**
 * Atrapa los errores de las dos fuentes posibles (Firestore y el backend
 * propio), así cambiar el LikeDataSource inyectado no cambia los mensajes.
 */
class LikeRepositoryImpl @Inject constructor(
    private val likeDataSource: LikeDataSource,
    private val notificationRepository: NotificationRepository
) : LikeRepository {

    /**
     * Si el like se guarda, se le avisa al autor de la reseña. La regla vive
     * aquí y no en cada ViewModel que tiene un corazón: dar like siempre
     * notifica, desde la pantalla que sea. Quitar el like no notifica.
     */
    override suspend fun likeReview(
        reviewId: String,
        userId: String,
        reviewAuthorId: String,
        vehicleId: String?
    ): Result<Unit> {
        val result = safeLikeCall { likeDataSource.likeReview(reviewId, userId) }
        if (result.isSuccess) {
            notificationRepository.notify(
                recipientUid = reviewAuthorId,
                type = NotificationType.LIKE,
                actorId = userId,
                vehicleId = vehicleId,
                reviewId = reviewId
            )
        }
        return result
    }

    override suspend fun unlikeReview(reviewId: String, userId: String): Result<Unit> =
        safeLikeCall { likeDataSource.unlikeReview(reviewId, userId) }

    /** No bloquea la pantalla: si falla, las reseñas se ven sin like marcado. */
    override fun observeLikedReviewIds(userId: String, reviewIds: List<String>): Flow<Set<String>> =
        likeDataSource.observeLikedReviewIds(userId, reviewIds).catch { emit(emptySet()) }

    private suspend fun safeLikeCall(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toLikeMessage()))
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(e.toSpanishMessage(notFound = REVIEW_NOT_FOUND, fallback = LIKE_ERROR))
            )
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }

    private fun FirebaseFirestoreException.toLikeMessage(): String = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sin conexión a internet, verifica tu red"
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para dar like"
        FirebaseFirestoreException.Code.NOT_FOUND -> REVIEW_NOT_FOUND
        else -> LIKE_ERROR
    }

    private companion object {
        const val REVIEW_NOT_FOUND = "Este review ya no existe"
        const val LIKE_ERROR = "No se pudo dar like, intenta de nuevo"
    }
}
