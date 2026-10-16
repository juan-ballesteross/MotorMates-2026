package com.example.motormates.data.dto

import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewVehicleRef
import com.google.firebase.firestore.DocumentId
import java.time.Instant

/**
 * Documento de la colección "reviews". Firestore no tiene JOINs, así que
 * el autor (user) y el vehículo (vehicle) se guardan desnormalizados dentro
 * de la reseña al momento de crearla. Si el autor cambia luego su nombre o
 * foto, las reseñas viejas conservan la captura anterior (consistencia
 * eventual), un costo aceptable a cambio de leer todo en una sola consulta.
 */
data class ReviewFirestoreDto(
    @DocumentId val id: String = "",
    val rating: Int = 0,
    val comment: String? = null,
    val userId: String = "",
    val vehicleId: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val user: ReviewUserSnapshot? = null,
    val vehicle: ReviewVehicleSnapshot? = null
) : ReviewDtoGeneric {

    override fun toDomain(): Review = Review(
        id = id,
        rating = rating,
        comment = comment,
        userId = userId,
        vehicleId = vehicleId,
        // El dominio maneja createdAt como ISO-8601 (lo que ya entiende
        // toRelativeTimeEs); en Firestore se guarda como epoch en millis.
        createdAt = if (createdAt > 0) Instant.ofEpochMilli(createdAt).toString() else null,
        author = user?.let { ReviewAuthor(id = userId, fullName = it.fullName, avatarUrl = it.avatarUrl) },
        vehicle = vehicle?.let { ReviewVehicleRef(id = vehicleId, brand = it.brand, model = it.model) }
    )
}

data class ReviewUserSnapshot(
    val fullName: String = "",
    val avatarUrl: String? = null
)

data class ReviewVehicleSnapshot(
    val brand: String = "",
    val model: String = ""
)
