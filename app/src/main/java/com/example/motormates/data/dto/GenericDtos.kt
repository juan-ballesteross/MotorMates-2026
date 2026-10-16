package com.example.motormates.data.dto

import com.example.motormates.data.model.Review
import com.example.motormates.data.model.Vehicle

/**
 * Contratos comunes para los DTOs de cada fuente de datos (Retrofit y
 * Firestore). Cada DTO concreto sabe convertirse al modelo de dominio con
 * sus propios nombres de campo, así el repositorio no depende de cuál
 * fuente esté inyectada.
 */
interface VehicleDtoGeneric {
    fun toDomain(): Vehicle
}

interface ReviewDtoGeneric {
    fun toDomain(): Review
}
