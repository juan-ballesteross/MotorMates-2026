package com.example.motormates.data.model

enum class ReviewAspect {
    COMFORT,
    DESIGN,
    PRICE,
    PERFORMANCE
}

/** Cabecera del auto que se está reseñando, en la pantalla de nueva reseña. */
data class ReviewCarSummary(
    val title: String,
    val year: Int,
    val categoryLabel: String,
    val imageUrl: String?
)

/** Una reseña vista desde un perfil: lo que importa es el auto, no el autor. */
data class UserReviewUi(
    val id: Int,
    val vehicleId: Int,
    val vehicleName: String,
    val rating: Int,
    val comment: String,
    val timeAgo: String
)

fun SearchCategoryKey.toSingularLabel(): String = when (this) {
    SearchCategoryKey.ALL -> ""
    SearchCategoryKey.SPORT -> "Deportivo"
    SearchCategoryKey.SUV -> "SUV"
    SearchCategoryKey.CLASSIC -> "Clásico"
    SearchCategoryKey.ELECTRIC -> "Eléctrico"
    SearchCategoryKey.PICKUP -> "Pickup"
}
