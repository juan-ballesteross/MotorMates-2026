package com.example.motormates.data.util

import java.time.Duration
import java.time.Instant

/**
 * Convierte el createdAt ISO-8601 que manda Sequelize en un texto relativo.
 * Vive acá y no en la pantalla para no meter lógica de formato en la UI.
 * minSdk 26 trae java.time sin necesidad de desugaring.
 */
fun String?.toRelativeTimeEs(): String {
    if (this.isNullOrBlank()) return ""
    val instant = try {
        Instant.parse(this)
    } catch (e: Exception) {
        // Un formato inesperado no debe tumbar un mapper: se devuelve vacío.
        return ""
    }

    val minutes = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 60 * 24 -> "hace ${minutes / 60} h"
        else -> {
            val days = minutes / (60 * 24)
            if (days == 1L) "hace 1 día" else "hace $days días"
        }
    }
}
