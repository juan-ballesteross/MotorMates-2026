package com.example.motormates.data.mock

import com.example.motormates.data.model.CarListing
import com.example.motormates.data.model.SearchCategoryKey

object SearchMocks {
    val sampleSearchCars = listOf(
        CarListing(1, "Porsche", "911 GT3", 2024, "Marco F.", 4.9f, 284, SearchCategoryKey.SPORT, VehicleImages.PORSCHE_911_GT3),
        CarListing(2, "Toyota", "Land Cruiser", 2022, "Elena R.", 4.7f, 156, SearchCategoryKey.SUV, VehicleImages.TOYOTA_LAND_CRUISER),
        CarListing(3, "Ford", "Mustang '67", 1967, "Diego M.", 4.8f, 342, SearchCategoryKey.CLASSIC, VehicleImages.FORD_MUSTANG_67),
        CarListing(4, "Tesla", "Model 3", 2024, "Sofía T.", 4.6f, 198, SearchCategoryKey.ELECTRIC, VehicleImages.TESLA_MODEL_3),
        CarListing(5, "Chevrolet", "Camaro SS", 2021, "Andrés P.", 4.8f, 267, SearchCategoryKey.SPORT, VehicleImages.CHEVROLET_CAMARO_SS),
        CarListing(6, "Jeep", "Wrangler", 2023, "Valentina C.", 4.5f, 132, SearchCategoryKey.SUV, VehicleImages.JEEP_WRANGLER),
        CarListing(7, "Volkswagen", "Beetle '65", 1965, "Camilo R.", 4.9f, 401, SearchCategoryKey.CLASSIC, VehicleImages.VOLKSWAGEN_BEETLE_65),
        CarListing(8, "Ford", "Ranger", 2022, "Laura G.", 4.4f, 98, SearchCategoryKey.PICKUP, VehicleImages.FORD_RANGER)
    )

    fun findById(id: Int): CarListing? = sampleSearchCars.find { it.id == id }
}
