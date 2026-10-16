package com.example.motormates.data.seed

import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.ReviewVehicleSnapshot
import com.example.motormates.data.source.REVIEWS_COLLECTION
import com.example.motormates.data.source.USERS_COLLECTION
import com.example.motormates.data.source.VEHICLES_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Siembra la base de datos con los usuarios, vehículos y reseñas de
 * SeedData la primera vez que se abre la app (si "vehicles" está vacía).
 * Todo va en un solo WriteBatch: o se escribe completo o no se escribe nada.
 */
@Singleton
class FirestoreSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun seedIfEmpty() {
        val alreadySeeded = !firestore.collection(VEHICLES_COLLECTION)
            .limit(1).get().await()
            .isEmpty
        if (alreadySeeded) return

        val usersById = SeedData.users.associateBy { it.id }
        val vehiclesById = SeedData.vehicles.associateBy { it.id }
        val now = System.currentTimeMillis()
        val batch = firestore.batch()

        SeedData.users.forEach { user ->
            batch.set(firestore.collection(USERS_COLLECTION).document(user.id), user)
        }
        SeedData.vehicles.forEach { vehicle ->
            batch.set(firestore.collection(VEHICLES_COLLECTION).document(vehicle.id), vehicle)
        }
        SeedData.reviews.forEachIndexed { index, seed ->
            val author = usersById.getValue(seed.userId)
            val vehicle = vehiclesById.getValue(seed.vehicleId)
            // Fechas escalonadas (una cada 5 h hacia atrás) para que el
            // "hace X" de cada reseña no sea siempre el mismo.
            val createdAt = now - index * FIVE_HOURS_MS
            val review = ReviewFirestoreDto(
                rating = seed.rating,
                comment = seed.comment,
                userId = seed.userId,
                vehicleId = seed.vehicleId,
                createdAt = createdAt,
                updatedAt = createdAt,
                user = ReviewUserSnapshot(fullName = author.fullName, avatarUrl = author.avatarUrl),
                vehicle = ReviewVehicleSnapshot(brand = vehicle.brand, model = vehicle.model)
            )
            batch.set(
                firestore.collection(REVIEWS_COLLECTION).document("seed_review_${index + 1}"),
                review
            )
        }

        batch.commit().await()
    }

    private companion object {
        const val FIVE_HOURS_MS = 5 * 60 * 60 * 1000L
    }
}
