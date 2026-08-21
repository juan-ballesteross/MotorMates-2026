package com.example.motormates.ui.review

import com.example.motormates.R
import com.example.motormates.ui.carDetail.CarDetailUi
import com.example.motormates.ui.search.model.CarListing
import com.example.motormates.ui.search.model.SearchCategoryKey

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
    imageResId = thumbnailResId()
)

fun CarListing.toCarDetailUi(): CarDetailUi = CarDetailUi(
    marca = brand.uppercase(),
    modelo = model,
    imagenResId = thumbnailResId(),
    calificacion = rating,
    numeroResenas = likes.coerceAtLeast(1),
    potencia = "—",
    aceleracion = "—",
    velocidadMaxima = "—",
    traccion = "—"
)

private fun CarListing.thumbnailResId(): Int =
    if (brand.contains("Porsche", ignoreCase = true)) R.drawable.porsche_gt3_rs else 0

fun SearchCategoryKey.toSingularLabel(): String = when (this) {
    SearchCategoryKey.ALL -> ""
    SearchCategoryKey.SPORT -> "Deportivo"
    SearchCategoryKey.SUV -> "SUV"
    SearchCategoryKey.CLASSIC -> "Clásico"
    SearchCategoryKey.ELECTRIC -> "Eléctrico"
    SearchCategoryKey.PICKUP -> "Pickup"
}
