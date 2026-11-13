package com.example.motormates.data.source

import android.net.Uri
import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.ReviewLocationSnapshot
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.ReviewVehicleSnapshot
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.remote.datasource.ReviewRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await


class FirestoreReviewDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ReviewRemoteDataSource {

    private val reviews get() = firestore.collection(REVIEWS_COLLECTION)

    override suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric> =
        findBy("vehicleId", vehicleId)

    override suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric> =
        findBy("userId", userId)

    override suspend fun createReview(review: NewReview): ReviewDtoGeneric {
        val now = System.currentTimeMillis()
        // Se genera el id antes de guardar (como con las historias) para
        // poder subir las fotos a "reviews/{id}/..." y guardar sus URLs ya
        // resueltas en el mismo documento, en vez de hacer un update aparte.
        val reviewId = reviews.document().id
        val imageUrls = uploadImages(reviewId, review.imageUris)
        val dto = ReviewFirestoreDto(
            id = reviewId,
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
            ),
            // Si no hay ubicación el campo no se escribe (queda null): la
            // reseña existe igual, solo que no sale en el mapa.
            location = review.location?.let {
                ReviewLocationSnapshot(lat = it.lat, lng = it.lng, placeName = it.placeName)
            },
            imageUrls = imageUrls
        )
        reviews.document(reviewId).set(dto).await()
        return dto
    }

    /**
     * Solo toca los campos que llegan, así que la ubicación original
     * sobrevive intacta a una edición: editar el texto de una reseña no
     * cambia el lugar donde se publicó.
     */
    override suspend fun updateReview(
        id: String,
        rating: Int?,
        comment: String?,
        keepImageUrls: List<String>,
        newImageUris: List<Uri>
    ): ReviewDtoGeneric {
        val uploadedUrls = uploadImages(id, newImageUris)
        val changes = mutableMapOf<String, Any>(
            "updatedAt" to System.currentTimeMillis(),
            "imageUrls" to keepImageUrls + uploadedUrls
        )
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

    /** Una subida por foto, en paralelo, igual que withCurrentAuthors en el repositorio. */
    private suspend fun uploadImages(reviewId: String, imageUris: List<Uri>): List<String> = coroutineScope {
        imageUris.map { uri ->
            async {
                val imageRef = storage.reference.child("reviews/$reviewId/${UUID.randomUUID()}.jpg")
                imageRef.putFile(uri).await()
                imageRef.downloadUrl.await().toString()
            }
        }.awaitAll()
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
