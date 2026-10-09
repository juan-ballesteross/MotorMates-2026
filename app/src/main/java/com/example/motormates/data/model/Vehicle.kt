package com.example.motormates.data.model

/**
 * Modelo de dominio de un vehículo. Es lo que devuelven los repositorios;
 * los DTOs no pasan de la capa de datos.
 *
 * Las especificaciones se propagan tal cual vienen del backend: ya traen su
 * unidad incluida ("502 hp", "3.4 s"), así que el dominio no las formatea.
 */
data class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val imageUrl: String?,
    val potencia: String?,
    val aceleracion: String?,
    val velocidadMaxima: String?,
    val traccion: String?
)
