package com.example.motormates.data.mock

import com.example.motormates.data.model.CarListing
import com.example.motormates.data.model.SearchCategoryKey

object SearchMocks {
    val sampleSearchCars = listOf(
        CarListing(1, "Porsche", "911 GT3", 2024, "Marco F.", 4.9f, 284, SearchCategoryKey.SPORT),
        CarListing(2, "Toyota", "Land Cruiser", 2022, "Elena R.", 4.7f, 156, SearchCategoryKey.SUV),
        CarListing(3, "Ford", "Mustang '67", 1967, "Diego M.", 4.8f, 342, SearchCategoryKey.CLASSIC),
        CarListing(4, "Tesla", "Model 3", 2024, "Sofía T.", 4.6f, 198, SearchCategoryKey.ELECTRIC),
        CarListing(5, "Chevrolet", "Camaro SS", 2021, "Andrés P.", 4.8f, 267, SearchCategoryKey.SPORT),
        CarListing(6, "Jeep", "Wrangler", 2023, "Valentina C.", 4.5f, 132, SearchCategoryKey.SUV),
        CarListing(7, "Volkswagen", "Beetle '65", 1965, "Camilo R.", 4.9f, 401, SearchCategoryKey.CLASSIC),
        CarListing(8, "Ford", "Ranger", 2022, "Laura G.", 4.4f, 98, SearchCategoryKey.PICKUP)
    )

    fun findById(id: Int): CarListing? = sampleSearchCars.find { it.id == id }
}
