package com.example.motormates.data.util

import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.model.ReviewVehicleRef
import com.example.motormates.data.model.Vehicle
import java.time.Instant

/** Datos fijos para las pruebas del ranking, las sugerencias y el onboarding. */
internal object OnboardingTestData {

    /** 2026-11-26 12:00 UTC. */
    const val NOW = 1_795_694_400_000L
    const val HOUR = 60L * 60 * 1000

    val BOGOTA = ReviewLocation(4.6766, -74.0482, "Parque 93, Bogotá")
    val MEDELLIN = ReviewLocation(6.2088, -75.5670, "El Poblado, Medellín")

    fun vehicle(
        id: String,
        brand: String,
        category: String,
        ownerId: String? = null,
        model: String = "Modelo $id"
    ) = Vehicle(
        id = id,
        brand = brand,
        model = model,
        year = 2024,
        category = category,
        imageUrl = "https://example.com/$id.jpg",
        potencia = null,
        aceleracion = null,
        velocidadMaxima = null,
        traccion = null,
        ownerId = ownerId
    )

    fun review(
        id: String,
        vehicleId: String,
        userId: String = "autor",
        rating: Int = 4,
        hoursAgo: Long = 2,
        likes: Int = 0,
        location: ReviewLocation? = BOGOTA,
        authorName: String = "Autor $userId",
        brand: String = "Marca"
    ) = Review(
        id = id,
        rating = rating,
        comment = "Comentario $id",
        userId = userId,
        vehicleId = vehicleId,
        createdAt = Instant.ofEpochMilli(NOW - hoursAgo * HOUR).toString(),
        likesCount = likes,
        author = ReviewAuthor(id = userId, fullName = authorName),
        vehicle = ReviewVehicleRef(id = vehicleId, brand = brand, model = "Modelo $vehicleId"),
        location = location
    )
}
