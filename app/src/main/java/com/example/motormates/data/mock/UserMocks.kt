package com.example.motormates.data.mock

import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.UserProfile

object UserMocks {
    val sampleUserProfile = UserProfile(
        name = "Rodrigo Salinas",
        handle = "@rodrigo.drives",
        location = "Ciudad de México",
        bio = "Coleccionista de deportivos alemanes. Fines de semana en el circuito.",
        reviewsCount = 47,
        followersDisplay = "1.2K",
        followingCount = 186
    )

    val sampleUserCars = listOf(
        GarageCar("911 GT3", VehicleImages.PORSCHE_911_GT3, year = 2024, categoryLabel = "Deportivo"),
        GarageCar("M3 Competition", VehicleImages.M3_COMPETITION, year = 2023, categoryLabel = "Deportivo")
    )
}
