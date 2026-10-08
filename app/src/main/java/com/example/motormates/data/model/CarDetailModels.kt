package com.example.motormates.data.model

/**
 * Entidades de UI (no son las entidades de la BD) — representan
 * exactamente lo que se muestra en la pantalla de detalle de auto.
 */
data class CarDetailUi(
    val marca: String,
    val modelo: String,
    val anio: Int,
    val categoria: String,
    val imageUrl: String?,
    val calificacion: Float,
    val numeroResenas: Int,
    val potencia: String,
    val aceleracion: String,
    val velocidadMaxima: String,
    val traccion: String
)

/**
 * Una reseña lista para pintar. isMine lo resuelve el mapper comparando
 * contra CURRENT_USER_ID, para que ninguna pantalla tenga que deducirlo.
 * avatarUrl va nulable porque el backend no guarda fotos de usuario.
 */
data class ReviewUi(
    val id: Int,
    val userId: Int,
    val nombreUsuario: String,
    val avatarUrl: String? = null,
    val tiempoTexto: String,
    val calificacion: Int,
    val comentario: String,
    val isMine: Boolean = false
)
