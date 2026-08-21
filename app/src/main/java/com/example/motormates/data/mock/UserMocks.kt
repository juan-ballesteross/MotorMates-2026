package com.example.motormates.data.mock

import com.example.motormates.R
import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.UserProfile

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
    GarageCar("911 GT3", R.drawable.gt3),
    GarageCar("M3 Competition", R.drawable.m3)
)
