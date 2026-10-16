package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.VehicleDtoGeneric

/**
 * Contrato de la fuente remota de vehículos. Existe como interfaz para
 * poder cambiar Retrofit por Firestore sin tocar el repositorio: basta con
 * cambiar el @Binds en NetworkModule.
 */
interface VehicleRemoteDataSource {

    suspend fun getVehicles(): List<VehicleDtoGeneric>

    /** Lanza NoSuchElementException (o HttpException 404) si no existe. */
    suspend fun getVehicleById(id: String): VehicleDtoGeneric
}
