package com.example.motormates.ui.search.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.example.motormates.R

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

/**
 * Empareja cada [SearchCategoryKey] con su etiqueta localizada de
 * res/values/strings.xml (mismo orden que R.array.search_categories),
 * separando la clave de negocio usada para filtrar del texto mostrado.
 */
@Composable
fun rememberSearchCategories(): List<SearchCategory> {
    val labels = stringArrayResource(R.array.search_categories)
    return SearchCategoryKey.entries.zip(labels) { key, label -> SearchCategory(key, label) }
}

val sampleSearchCars = listOf(
    CarListing("Porsche", "911 GT3", 2024, "Marco F.", 4.9f, 284, SearchCategoryKey.SPORT),
    CarListing("Toyota", "Land Cruiser", 2022, "Elena R.", 4.7f, 156, SearchCategoryKey.SUV),
    CarListing("Ford", "Mustang '67", 1967, "Diego M.", 4.8f, 342, SearchCategoryKey.CLASSIC),
    CarListing("Tesla", "Model 3", 2024, "Sofía T.", 4.6f, 198, SearchCategoryKey.ELECTRIC),
    CarListing("Chevrolet", "Camaro SS", 2021, "Andrés P.", 4.8f, 267, SearchCategoryKey.SPORT),
    CarListing("Jeep", "Wrangler", 2023, "Valentina C.", 4.5f, 132, SearchCategoryKey.SUV),
    CarListing("Volkswagen", "Beetle '65", 1965, "Camilo R.", 4.9f, 401, SearchCategoryKey.CLASSIC),
    CarListing("Ford", "Ranger", 2022, "Laura G.", 4.4f, 98, SearchCategoryKey.PICKUP)
)
