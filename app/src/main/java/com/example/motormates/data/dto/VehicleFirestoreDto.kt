package com.example.motormates.data.dto

import com.example.motormates.data.model.Vehicle
import com.google.firebase.firestore.DocumentId

/**
 * Documento de la colección "vehicles". Todos los campos tienen valor por
 * defecto: Firestore necesita un constructor sin argumentos para
 * deserializar con toObject().
 */
data class VehicleFirestoreDto(
    @DocumentId val id: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val category: String = "",
    val imageUrl: String? = null,
    val potencia: String? = null,
    val aceleracion: String? = null,
    val velocidadMaxima: String? = null,
    val traccion: String? = null
) : VehicleDtoGeneric {

    override fun toDomain(): Vehicle = Vehicle(
        id = id,
        brand = brand,
        model = model,
        year = year,
        category = category,
        imageUrl = imageUrl,
        potencia = potencia,
        aceleracion = aceleracion,
        velocidadMaxima = velocidadMaxima,
        traccion = traccion
    )
}
