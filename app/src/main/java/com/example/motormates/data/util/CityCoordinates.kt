package com.example.motormates.data.util

import java.text.Normalizer
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Una ciudad que se puede elegir en el onboarding, con su centro. */
data class City(val name: String, val lat: Double, val lng: Double)

/**
 * Las ciudades del onboarding: las mismas del seed. Las coordenadas son el
 * centro aproximado de cada una; sirven para centrar el mapa y para saber si
 * una reseña se publicó "en tu ciudad" (a menos de RADIUS_KM).
 */
object CityCoordinates {

    val CITIES = listOf(
        City("Bogotá", 4.6533, -74.0836),
        City("Medellín", 6.2442, -75.5812),
        City("Cali", 3.4516, -76.5320),
        City("Barranquilla", 10.9685, -74.7813),
        City("Bucaramanga", 7.1193, -73.1227),
        City("Cartagena", 10.3910, -75.4794),
        City("Pereira", 4.8133, -75.6961),
        City("Manizales", 5.0703, -75.5138)
    )

    /** Un área metropolitana cabe holgada en este radio, y dos ciudades no se tocan. */
    const val RADIUS_KM = 30.0

    fun find(name: String?): City? {
        val key = name?.normalizedKey()?.takeIf { it.isNotEmpty() } ?: return null
        return CITIES.firstOrNull { it.name.normalizedKey() == key }
    }

    /**
     * La ciudad dentro de un texto libre como "Medellín, Colombia" (el campo
     * location del perfil). Sirve para prellenar el onboarding.
     */
    fun findIn(text: String?): City? {
        val key = text?.normalizedKey() ?: return null
        return CITIES.firstOrNull { key.contains(it.name.normalizedKey()) }
    }

    /** La ciudad más cercana, si el punto cae dentro de su radio. */
    fun nearest(lat: Double, lng: Double): City? =
        CITIES.map { it to distanceKm(lat, lng, it.lat, it.lng) }
            .filter { (_, km) -> km <= RADIUS_KM }
            .minByOrNull { (_, km) -> km }
            ?.first

    fun isInCity(lat: Double, lng: Double, city: City): Boolean =
        distanceKm(lat, lng, city.lat, city.lng) <= RADIUS_KM

    /** Haversine: distancia sobre la superficie de la Tierra. */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    private const val EARTH_RADIUS_KM = 6371.0
}

/** Minúsculas y sin tildes: "Bogotá" y "bogota" son la misma ciudad (y la misma marca). */
internal fun String.normalizedKey(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
