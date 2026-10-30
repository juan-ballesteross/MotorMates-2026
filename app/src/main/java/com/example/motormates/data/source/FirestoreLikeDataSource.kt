package com.example.motormates.data.source

import com.example.motormates.data.dto.LikeDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await


class FirestoreLikeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : LikeDataSource {

    /**
     * Una transacción: o se hacen las dos escrituras (crear el like y sumar
     * 1 al contador) o no se hace ninguna. Si el like ya existía, no se
     * vuelve a sumar, así un doble toque no descuadra el contador.
     */
    override suspend fun likeReview(reviewId: String, userId: String) {
        val reviewRef = firestore.collection(REVIEWS_COLLECTION).document(reviewId)
        val likeRef = reviewRef.collection(LIKES_COLLECTION).document(userId)

        firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)

            if (!likeSnapshot.exists()) {
                transaction.set(
                    likeRef,
                    LikeDto(userId = userId, createdAt = System.currentTimeMillis())
                )
                // Si el review no existe, este update falla con NOT_FOUND y se
                // cancela también el set del like. increment trata un campo
                // inexistente como 0, así que sirve con los reviews cargados a mano.
                transaction.update(reviewRef, "likesCount", FieldValue.increment(1))
            }

            // Una transacción debe terminar devolviendo un valor; no necesitamos ninguno.
            null
        }.await()
    }

    override suspend fun unlikeReview(reviewId: String, userId: String) {
        val reviewRef = firestore.collection(REVIEWS_COLLECTION).document(reviewId)
        val likeRef = reviewRef.collection(LIKES_COLLECTION).document(userId)

        firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)

            if (likeSnapshot.exists()) {
                transaction.delete(likeRef)
                transaction.update(reviewRef, "likesCount", FieldValue.increment(-1))
            }

            null
        }.await()
    }

    /**
     * Un listener por reseña sobre reviews/{id}/likes/{uid} (no un
     * collectionGroup, que exige crear un índice en la consola): las listas
     * que se muestran son cortas. combine junta todos en un Set y vuelve a
     * emitir apenas cualquiera cambia, incluso si el like se dio o se quitó
     * desde otro dispositivo.
     */
    override fun observeLikedReviewIds(userId: String, reviewIds: List<String>): Flow<Set<String>> {
        if (reviewIds.isEmpty()) return flowOf(emptySet())

        val perReview = reviewIds.map { reviewId ->
            observeLikeExists(reviewId, userId).map { liked -> if (liked) reviewId else null }
        }
        return combine(perReview) { results -> results.filterNotNull().toSet() }
    }

    /** Mismo molde que observeQuery en ReviewRealtimeDataSource. */
    private fun observeLikeExists(reviewId: String, userId: String): Flow<Boolean> = callbackFlow {
        val registration = firestore.collection(REVIEWS_COLLECTION)
            .document(reviewId)
            .collection(LIKES_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() == true)
            }
        awaitClose { registration.remove() }
    }
}