package com.example.motormates.data.remote.mapper

import com.example.motormates.data.model.BackendUser
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewVehicleRef
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.remote.dto.ReviewDto
import com.example.motormates.data.remote.dto.ReviewUserDto
import com.example.motormates.data.remote.dto.ReviewVehicleDto
import com.example.motormates.data.remote.dto.UserDto
import com.example.motormates.data.remote.dto.VehicleDto

/**
 * Funciones de mapeo DTO de Retrofit -> dominio. Están aisladas acá para
 * que la conversión ocurra en un solo lugar y no se repita en cada
 * repositorio. Los ids numéricos del backend se pasan a String, que es el
 * tipo de id del dominio (compartido con Firestore).
 */

internal fun VehicleDto.mapToDomain(): Vehicle = Vehicle(
    id = id.toString(),
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

fun UserDto.toDomain(): BackendUser = BackendUser(
    id = id,
    fullName = fullName,
    email = email
)

fun ReviewUserDto.toDomain(): ReviewAuthor = ReviewAuthor(id = id.toString(), fullName = fullName)

fun ReviewVehicleDto.toDomain(): ReviewVehicleRef =
    ReviewVehicleRef(id = id.toString(), brand = brand, model = model)

internal fun ReviewDto.mapToDomain(): Review = Review(
    id = id.toString(),
    rating = rating,
    comment = comment,
    userId = userId.toString(),
    vehicleId = vehicleId.toString(),
    createdAt = createdAt,
    author = user?.toDomain(),
    vehicle = vehicle?.toDomain()
)
