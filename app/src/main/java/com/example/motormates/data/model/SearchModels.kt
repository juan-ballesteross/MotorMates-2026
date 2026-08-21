package com.example.motormates.data.model

enum class SearchCategoryKey { ALL, SPORT, SUV, CLASSIC, ELECTRIC, PICKUP }

data class SearchCategory(val key: SearchCategoryKey, val label: String)

data class CarListing(
    val brand: String,
    val model: String,
    val year: Int,
    val ownerName: String,
    val rating: Float,
    val likes: Int,
    val category: SearchCategoryKey
)
