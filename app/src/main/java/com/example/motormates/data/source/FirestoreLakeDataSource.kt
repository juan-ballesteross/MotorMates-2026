package com.example.motormates.data.source

import com.example.motormates.data.dto.LikeDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
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
}