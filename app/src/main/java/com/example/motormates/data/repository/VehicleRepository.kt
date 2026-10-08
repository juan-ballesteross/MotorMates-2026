package com.example.motormates.data.repository

import com.example.motormates.data.remote.VehicleRetrofitDataSource
import com.example.motormates.data.remote.dto.VehicleDto
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

interface VehicleRepository {
    suspend fun getVehicles(): Result<List<VehicleDto>>
    suspend fun getVehicleById(id: Int): Result<VehicleDto>
}

class VehicleRepositoryImpl @Inject constructor(
    private val remoteDataSource: VehicleRetrofitDataSource
) : VehicleRepository {

    // ... getVehicles() y getVehicleById() sin cambios
}