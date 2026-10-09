package com.example.motormates.data.remote.service

import com.example.motormates.data.remote.dto.VehicleDto
import retrofit2.http.GET
import retrofit2.http.Path

interface VehicleApiService {

    @GET("vehicles")
    suspend fun getVehicles(): List<VehicleDto>

    @GET("vehicles/{id}")
    suspend fun getVehicleById(@Path("id") id: Int): VehicleDto
}
