package com.example.motormates.data.model

enum class ReviewAspect {
    COMFORT,
    DESIGN,
    PRICE,
    PERFORMANCE
}

data class ReviewCarSummary(
    val title: String,
    val year: Int,
    val categoryLabel: String,
    val imageResId: Int
)

fun CarListing.toReviewCarSummary(): ReviewCarSummary = ReviewCarSummary(
    title = "$brand $model",
    year = year,
    categoryLabel = category.toSingularLabel(),
    imageResId = imageResId
)

fun CarListing.toCarDetailUi(): CarDetailUi = CarDetailUi(
    marca = brand.uppercase(),
    modelo = model,
    imagenResId = imageResId,
    calificacion = rating,
    numeroResenas = likes.coerceAtLeast(1),
    potencia = "—",
    aceleracion = "—",
    velocidadMaxima = "—",
    traccion = "—"
)

fun SearchCategoryKey.toSingularLabel(): String = when (this) {
    SearchCategoryKey.ALL -> ""
    SearchCategoryKey.SPORT -> "Deportivo"
    SearchCategoryKey.SUV -> "SUV"
    SearchCategoryKey.CLASSIC -> "Clásico"
    SearchCategoryKey.ELECTRIC -> "Eléctrico"
    SearchCategoryKey.PICKUP -> "Pickup"
}
