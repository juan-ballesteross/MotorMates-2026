package com.example.motormates.data.source

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.ReviewVehicleSnapshot
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.remote.datasource.ReviewRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await


class FirestoreReviewDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRemoteDataSource {

    private val reviews get() = firestore.collection(REVIEWS_COLLECTION)

    override suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric> =
        findBy("vehicleId", vehicleId)

    override suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric> =
        findBy("userId", userId)

    override suspend fun createReview(review: NewReview): ReviewDtoGeneric {
        val now = System.currentTimeMillis()
        val dto = ReviewFirestoreDto(
            rating = review.rating,
            comment = review.comment,
            userId = review.userId,
            vehicleId = review.vehicleId,
            createdAt = now,
            updatedAt = now,
            user = ReviewUserSnapshot(
                fullName = review.authorName,
                avatarUrl = review.authorAvatarUrl
            ),
            vehicle = ReviewVehicleSnapshot(
                brand = review.vehicleBrand,
                model = review.vehicleModel
            )
        )
        // add() deja que Firestore genere el id del documento.
        val ref = reviews.add(dto).await()
        return dto.copy(id = ref.id)
    }

    override suspend fun updateReview(id: String, rating: Int?, comment: String?): ReviewDtoGeneric {
        val changes = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        rating?.let { changes["rating"] = it }
        comment?.let { changes["comment"] = it }

        val ref = reviews.document(id)
        // update() falla con NOT_FOUND si el documento ya no existe.
        ref.update(changes).await()
        return ref.get().await().toObject(ReviewFirestoreDto::class.java)
            ?: throw NoSuchElementException("No existe la reseña $id")
    }

    override suspend fun deleteReview(id: String) {
        reviews.document(id).delete().await()
    }

    /**
     * Se ordena en memoria (más recientes primero) en vez de con orderBy:
     * whereEqualTo + orderBy sobre otro campo exige crear un índice
     * compuesto en la consola, y las listas por vehículo/usuario son cortas.
     */
    private suspend fun findBy(field: String, value: String): List<ReviewFirestoreDto> =
        reviews.whereEqualTo(field, value).get().await()
            .toObjects(ReviewFirestoreDto::class.java)
            .sortedByDescending { it.createdAt }
}
