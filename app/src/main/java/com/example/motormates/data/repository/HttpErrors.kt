package com.example.motormates.data.repository

import retrofit2.HttpException

/**
 * Traduce un código HTTP a un mensaje en español. Los repositorios lo usan
 * dentro del catch de HttpException para no filtrar el texto crudo de la
 * librería hacia la UI. notFound cambia según la operación.
 */
internal fun HttpException.toSpanishMessage(
    notFound: String,
    fallback: String
): String = when (code()) {
    400 -> "Los datos enviados no son válidos"
    404 -> notFound
    in 500..599 -> "El servidor no está disponible, intenta más tarde"
    else -> fallback
}

internal const val NO_CONNECTION_MESSAGE =
    "Sin conexión con el servidor, verifica que esté encendido"
