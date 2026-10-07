package com.example.motormates.data.remote;

import com.example.motormates.data.remote.dto.VehicleDto

interface VehicleRemoteDataSource {

    suspend fun getVehicles(): List<VehicleDto>

    suspend fun getVehicleById(id: Int): VehicleDto
}