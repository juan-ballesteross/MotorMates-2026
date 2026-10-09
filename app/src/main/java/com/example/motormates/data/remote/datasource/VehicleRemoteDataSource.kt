package com.example.motormates.data.remote.datasource

import com.example.motormates.data.remote.dto.VehicleDto

/**
 * Contrato de la fuente remota de vehículos. Existe como interfaz para
 * poder cambiar Retrofit por otra tecnología sin tocar el repositorio.
 */
interface VehicleRemoteDataSource {

    suspend fun getVehicles(): List<VehicleDto>

    suspend fun getVehicleById(id: Int): VehicleDto
}
