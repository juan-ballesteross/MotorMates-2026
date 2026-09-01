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

// toCarDetailUi() ya existe en ReviewModels.kt — no se duplica aquí.

// ===== Datos locales quemados (fallback mientras no hay id, o para Preview) =====

val mockCarDetail = CarDetailUi(
    marca = "PORSCHE",
    modelo = "911 GT3",
    imagenResId = 0,
    calificacion = 4.9f,
    numeroResenas = 312,
    potencia = "510 hp",
    aceleracion = "3.4 s",
    velocidadMaxima = "318 km/h",
    traccion = "RWD"
)

val mockReviews = listOf(
    ReviewUi(
        id = 1,
        nombreUsuario = "Sofía Reyes",
        avatarResId = 0,
        tiempoTexto = "hace 3 días",
        calificacion = 5,
        comentario = "El mejor GT3 que he probado. El escape en modo Sport es adictivo."
    ),
    ReviewUi(
        id = 2,
        nombreUsuario = "Iván Pérez",
        avatarResId = 0,
        tiempoTexto = "hace 5 días",
        calificacion = 5,
        comentario = "El mejor GT3 que he probado. El escape en modo Sport es adictivo."
    )
)