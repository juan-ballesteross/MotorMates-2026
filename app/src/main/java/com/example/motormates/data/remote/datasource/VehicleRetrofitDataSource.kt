package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.VehicleDtoGeneric
import com.example.motormates.data.remote.service.VehicleApiService
import javax.inject.Inject

class VehicleRetrofitDataSource @Inject constructor(
    private val service: VehicleApiService
) : VehicleRemoteDataSource {

    override suspend fun getVehicles(): List<VehicleDtoGeneric> = service.getVehicles()

    override suspend fun getVehicleById(id: String): VehicleDtoGeneric = service.getVehicleById(id)
}
