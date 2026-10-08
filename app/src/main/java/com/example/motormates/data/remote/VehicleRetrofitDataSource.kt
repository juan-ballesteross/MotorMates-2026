package com.example.motormates.data.remote

import com.example.motormates.data.remote.dto.VehicleDto
import javax.inject.Inject

class VehicleRetrofitDataSource @Inject constructor(
    private val service: VehicleApiService
) : VehicleRemoteDataSource {

    override suspend fun getVehicles(): List<VehicleDto> = service.getVehicles()

    override suspend fun getVehicleById(id: Int): VehicleDto = service.getVehicleById(id)
}
