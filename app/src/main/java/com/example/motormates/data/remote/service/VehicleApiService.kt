package com.example.motormates.data.remote.service

import com.example.motormates.data.remote.dto.UpdateVehicleRequest
import retrofit2.http.DELETE
import retrofit2.http.PUT
import com.example.motormates.data.remote.dto.CreateVehicleRequest
import retrofit2.http.POST
import retrofit2.http.Body
import com.example.motormates.data.remote.dto.VehicleDto
import retrofit2.http.GET
import retrofit2.http.Path

interface VehicleApiService {

    @GET("vehicles")
    suspend fun getVehicles(): List<VehicleDto>

    @GET("vehicles/{id}")
    suspend fun getVehicleById(@Path("id") id: String): VehicleDto

    @POST("vehicles")
    suspend fun createVehicle(@Body body: CreateVehicleRequest): VehicleDto

    @PUT("vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: String, @Body body: UpdateVehicleRequest): VehicleDto

    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: String)
}
