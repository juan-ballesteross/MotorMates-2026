package com.example.motormates.data.util

import java.time.Duration
import java.time.Instant

/**
 * Convierte el createdAt ISO-8601 que manda Sequelize en un texto relativo.
 * Vive acá y no en la pantalla para no meter lógica de formato en la UI.
 * minSdk 26 trae java.time sin necesidad de desugaring.
 */
/**
 * El createdAt ISO-8601 de vuelta a epoch en millis, o null si no se puede
 * leer. Hace falta porque el dominio ya perdió los millis (los guarda como
 * texto ISO) y el mapa necesita comparar fechas para quedarse solo con las
 * reseñas de las últimas 24 h.
 *
 * No se comparan los textos ISO directamente: Instant.toString() escribe los
 * segundos fraccionarios con precisión variable ("...T10:00:00Z" vs
 * "...T10:00:00.123Z"), así que el orden lexicográfico no coincide con el
 * cronológico.
 */
fun String?.toEpochMillisOrNull(): Long? {
    if (this.isNullOrBlank()) return null
    return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
}

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
