package com.example.motormates.data.model

/**
 * Entidades de UI (no son las entidades de la BD) — representan
 * exactamente lo que se muestra en la pantalla de detalle de auto.
 */
data class CarDetailUi(
    val marca: String,
    val modelo: String,
    val imagenResId: Int,
    val calificacion: Float,
    val numeroResenas: Int,
    val potencia: String,
    val aceleracion: String,
    val velocidadMaxima: String,
    val traccion: String
)

data class ReviewUi(
    val id: Int,
    val nombreUsuario: String,
    val avatarResId: Int,
    val tiempoTexto: String,
    val calificacion: Int,
    val comentario: String
)
