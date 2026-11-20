package com.example.motormates.data.source

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Cuántas imágenes generó hoy cada usuario (documento aiUsage/{uid} con
 * {date: "2026-11-17", count: 3}). Cada imagen cuesta dinero en el plan
 * Blaze, así que se limita por día.
 */
interface AiUsageDataSource {
    /** Generaciones usadas hoy. */
    suspend fun usedToday(uid: String, today: String): Int

    /**
     * Suma una generación si no se pasa del límite, en una transacción.
     * @return true si se pudo consumir, false si ya se alcanzó el límite.
     */
    suspend fun tryConsume(uid: String, today: String, limit: Int): Boolean
}

class FirestoreAiUsageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : AiUsageDataSource {

    override suspend fun usedToday(uid: String, today: String): Int {
        val snapshot = firestore.collection(AI_USAGE_COLLECTION).document(uid).get().await()
        return if (snapshot.getString(FIELD_DATE) == today) {
            snapshot.getLong(FIELD_COUNT)?.toInt() ?: 0
        } else 0
    }

    override suspend fun tryConsume(uid: String, today: String, limit: Int): Boolean {
        val ref = firestore.collection(AI_USAGE_COLLECTION).document(uid)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            // Un día nuevo reinicia el contador.
            val used = if (snapshot.getString(FIELD_DATE) == today) {
                snapshot.getLong(FIELD_COUNT)?.toInt() ?: 0
            } else 0
            if (used >= limit) {
                false
            } else {
                transaction.set(ref, mapOf(FIELD_DATE to today, FIELD_COUNT to (used + 1).toLong()))
                true
            }
        }.await()
    }

    private companion object {
        const val FIELD_DATE = "date"
        const val FIELD_COUNT = "count"
    }
}
