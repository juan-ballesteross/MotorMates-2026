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
 * Funciones de mapeo DTO -> dominio. Están aisladas acá para que la
 * conversión ocurra en un solo lugar y no se repita en cada repositorio.
 */

fun VehicleDto.toDomain(): Vehicle = Vehicle(
    id = id,
    brand = brand,
    model = model,
    year = year,
    category = category,
    imageUrl = imageUrl
)

fun UserDto.toDomain(): BackendUser = BackendUser(
    id = id,
    fullName = fullName,
    email = email
)

fun ReviewUserDto.toDomain(): ReviewAuthor = ReviewAuthor(id = id, fullName = fullName)

fun ReviewVehicleDto.toDomain(): ReviewVehicleRef =
    ReviewVehicleRef(id = id, brand = brand, model = model)

fun ReviewDto.toDomain(): Review = Review(
    id = id,
    rating = rating,
    comment = comment,
    userId = userId,
    vehicleId = vehicleId,
    createdAt = createdAt,
    author = user?.toDomain(),
    vehicle = vehicle?.toDomain()
)
