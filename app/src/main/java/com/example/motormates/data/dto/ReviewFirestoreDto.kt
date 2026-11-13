package com.example.motormates.data.dto

import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewLocation
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
    val likesCount: Int = 0,
    val user: ReviewUserSnapshot? = null,
    val vehicle: ReviewVehicleSnapshot? = null,
    val location: ReviewLocationSnapshot? = null
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
        likesCount = likesCount,
        author = user?.let { ReviewAuthor(id = userId, fullName = it.fullName, avatarUrl = it.avatarUrl) },
        vehicle = vehicle?.let { ReviewVehicleRef(id = vehicleId, brand = it.brand, model = it.model) },
        location = location?.toDomain()
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

/**
 * El mapa { lat, lng } que se guarda dentro de la reseña. Se usa un mapa
 * anidado y no un GeoPoint de Firebase para que el DTO no se amarre a un tipo
 * de Firebase y para que el seeder de Node y el JSON del backend propio sigan
 * siendo triviales.
 *
 * Los dos campos son nulables (y no 0.0) a propósito: así un documento
 * incompleto o mal escrito se descarta en vez de convertirse en un marcador
 * en el golfo de Guinea, que es donde cae la coordenada (0, 0).
 */
data class ReviewLocationSnapshot(
    val lat: Double? = null,
    val lng: Double? = null,
    /** Nombre legible del sitio, resuelto al publicar ("Chapinero, Bogotá"). */
    val placeName: String? = null
) {
    fun toDomain(): ReviewLocation? =
        if (lat != null && lng != null) {
            ReviewLocation(lat = lat, lng = lng, placeName = placeName?.ifBlank { null })
        } else {
            null
        }
}
