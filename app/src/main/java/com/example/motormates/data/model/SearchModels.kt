package com.example.motormates.data.model

import com.example.motormates.data.util.normalizedKey

enum class SearchCategoryKey { ALL, SPORT, SUV, CLASSIC, ELECTRIC, PICKUP }

data class SearchCategory(val key: SearchCategoryKey, val label: String)

/** Un vehículo del catálogo real (Firestore), listo para la grilla del buscador. */
data class CarListing(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    /** Null si es del catálogo y no tiene dueño. */
    val ownerName: String?,
    val category: SearchCategoryKey,
    val categoryLabel: String,
    val imageUrl: String?
)

/** "Deportivo" -> SPORT. Lo que no se reconoce queda en ALL (sale solo sin filtro). */
fun searchCategoryOf(label: String): SearchCategoryKey = when (label.normalizedKey()) {
    "deportivo" -> SearchCategoryKey.SPORT
    "suv" -> SearchCategoryKey.SUV
    "clasico" -> SearchCategoryKey.CLASSIC
    "electrico" -> SearchCategoryKey.ELECTRIC
    "pickup" -> SearchCategoryKey.PICKUP
    else -> SearchCategoryKey.ALL
}

fun Vehicle.toCarListing(): CarListing = CarListing(
    id = id,
    brand = brand,
    model = model,
    year = year,
    ownerName = ownerName?.takeIf { ownerId != null && it.isNotBlank() },
    category = searchCategoryOf(category),
    categoryLabel = category,
    imageUrl = imageUrl
)

/** Resultado de buscar usuarios reales (Firestore) en el buscador. */
data class UserSearchResult(
    val uid: String,
    val name: String,
    val handle: String,
    val avatarUrl: String? = null
)
