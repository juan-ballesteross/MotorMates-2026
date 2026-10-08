package com.example.motormates.data.model

/**
 * Modelo de dominio de un vehículo. Es lo que devuelven los repositorios;
 * los DTOs no pasan de la capa de datos.
 */
data class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val imageUrl: String?
)
