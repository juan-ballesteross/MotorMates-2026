package com.example.motormates.data.dto

import com.example.motormates.data.model.CarRef
import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.MAX_PREFERENCE_SELECTION
import com.example.motormates.data.model.UserPreferences

/**
 * El mapa `preferences` dentro de users/{uid}. Todos los campos tienen valor
 * por defecto porque Firestore necesita un constructor sin argumentos, y
 * porque los usuarios creados antes del onboarding no lo tienen.
 */
data class PreferencesDto(
    val categories: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val city: String? = null,
    val car: CarRefDto? = null,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val updatedAt: Long = 0L
) {
    fun toDomain(): UserPreferences = UserPreferences(
        categories = categories.cleaned(),
        brands = brands.cleaned(),
        // Un interés que esta versión no conoce se ignora en vez de fallar.
        interests = interests.mapNotNull { Interest.fromKey(it) }.distinct(),
        city = city?.trim()?.takeIf { it.isNotEmpty() },
        car = car?.toDomain(),
        completed = completed,
        skipped = skipped,
        updatedAt = updatedAt
    )

    /** Lo que se escribe en Firestore (mapas y listas, sin clases propias). */
    fun toMap(): Map<String, Any?> = mapOf(
        "categories" to categories,
        "brands" to brands,
        "interests" to interests,
        "city" to city,
        "car" to car?.let { mapOf("brand" to it.brand, "model" to it.model, "year" to it.year) },
        "completed" to completed,
        "skipped" to skipped,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromDomain(preferences: UserPreferences): PreferencesDto = PreferencesDto(
            categories = preferences.categories.cleaned(),
            brands = preferences.brands.cleaned(),
            interests = preferences.interests.distinct().map { it.name },
            city = preferences.city,
            car = preferences.car?.let { CarRefDto(it.brand, it.model, it.year) },
            completed = preferences.completed,
            skipped = preferences.skipped,
            updatedAt = preferences.updatedAt
        )

        private fun List<String>.cleaned(): List<String> =
            map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_PREFERENCE_SELECTION)
    }
}

data class CarRefDto(
    val brand: String = "",
    val model: String = "",
    val year: Int = 0
) {
    fun toDomain(): CarRef? =
        if (brand.isBlank() || model.isBlank() || year <= 0) null else CarRef(brand.trim(), model.trim(), year)
}

/** Solo para leer el campo `preferences` del documento del usuario. */
data class UserPreferencesHolder(val preferences: PreferencesDto? = null)
