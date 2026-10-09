package com.example.motormates.data.repository

import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.remote.mapper.toDomain
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

interface VehicleRepository {
    suspend fun getVehicles(): Result<List<Vehicle>>
    suspend fun getVehicleById(id: Int): Result<Vehicle>
}

/**
 * Devuelve modelos de dominio, nunca DTOs: la capa de arriba no debe saber
 * cómo viene armado el JSON. Se inyecta la interfaz de la fuente de datos,
 * no la implementación de Retrofit.
 */
class VehicleRepositoryImpl @Inject constructor(
    private val remoteDataSource: VehicleRemoteDataSource
) : VehicleRepository {

    /**
     * Catches específicos primero y genérico al final, igual que en
     * AuthRepositoryImpl. Se usa try/catch con HttpException en vez de
     * Response<T> para no amarrar la app al tipo de respuesta de Retrofit.
     */
    override suspend fun getVehicles(): Result<List<Vehicle>> {
        return try {
            Result.success(remoteDataSource.getVehicles().map { it.toDomain() })
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "No se encontraron vehículos",
                        fallback = "No se pudieron cargar los vehículos"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudieron cargar los vehículos"))
        }
    }

    override suspend fun getVehicleById(id: Int): Result<Vehicle> {
        return try {
            Result.success(remoteDataSource.getVehicleById(id).toDomain())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Este vehículo no existe o fue eliminado",
                        fallback = "No se pudo cargar el vehículo"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar el vehículo"))
        }
    }
}
