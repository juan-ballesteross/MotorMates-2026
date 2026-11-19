package com.example.motormates.data.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Agrupar reseñas cercanas en el mapa.
 *
 * Kotlin puro (sin el SDK de Maps) para probarlo en la JVM. Lo usan la
 * pantalla, para dibujar un solo punto por grupo, y el ViewModel, para
 * decidir si tocar un grupo abre la lista o acerca la cámara.
 */

/** Reseñas a esta distancia o menos se ven como un solo punto, con cualquier zoom. */
const val NEARBY_GROUP_RADIUS_METERS = 30.0

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Distancia real sobre la Tierra entre dos puntos (fórmula de haversine), en metros. */
fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

private fun ReviewMapMarkerUi.distanceTo(other: ReviewMapMarkerUi): Double =
    distanceMeters(lat, lng, other.lat, other.lng)

/**
 * Junta las reseñas que están a radiusMeters o menos. Cada grupo arranca en
 * la primera reseña libre de la lista (la más reciente, en el orden en que
 * llegan) y se queda con todas las libres que estén a ese radio de ella. Así
 * el resultado es estable: la misma lista da siempre los mismos grupos.
 */
fun groupNearby(
    markers: List<ReviewMapMarkerUi>,
    radiusMeters: Double = NEARBY_GROUP_RADIUS_METERS
): List<List<ReviewMapMarkerUi>> {
    val remaining = markers.toMutableList()
    val groups = mutableListOf<List<ReviewMapMarkerUi>>()
    while (remaining.isNotEmpty()) {
        val seed = remaining.first()
        val group = remaining.filter { seed.distanceTo(it) <= radiusMeters }
        remaining.removeAll(group.toSet())
        groups += group
    }
    return groups
}

/** ¿Estas reseñas forman un solo grupo cercano? */
fun List<ReviewMapMarkerUi>.areNearbyGroup(radiusMeters: Double = NEARBY_GROUP_RADIUS_METERS): Boolean =
    isNotEmpty() && groupNearby(this, radiusMeters).size == 1
