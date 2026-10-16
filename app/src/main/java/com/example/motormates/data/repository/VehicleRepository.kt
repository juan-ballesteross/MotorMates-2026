package com.example.motormates.data.repository

import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

interface VehicleRepository {
    suspend fun getVehicles(): Result<List<Vehicle>>
    suspend fun getVehicleById(id: String): Result<Vehicle>
}

/**
 * Devuelve modelos de dominio, nunca DTOs: la capa de arriba no debe saber
 * cómo viene armado el dato. Se inyecta la interfaz de la fuente de datos,
 * no la implementación (hoy Firestore, antes Retrofit).
 */
class VehicleRepositoryImpl @Inject constructor(
    private val remoteDataSource: VehicleRemoteDataSource
) : VehicleRepository {

    /**
     * Catches específicos primero y genérico al final, igual que en
     * AuthRepositoryImpl. Se atrapan tanto los errores de Retrofit
     * (IOException/HttpException) como los de Firestore, para que cambiar
     * de fuente de datos no cambie los mensajes que ve la UI.
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
        } catch (e: FirebaseFirestoreException) {
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

    override suspend fun getVehicleById(id: String): Result<Vehicle> {
        return try {
            Result.success(remoteDataSource.getVehicleById(id).toDomain())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = VEHICLE_NOT_FOUND,
                        fallback = "No se pudo cargar el vehículo"
                    )
                )
            )
        } catch (e: FirebaseFirestoreException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = VEHICLE_NOT_FOUND,
                        fallback = "No se pudo cargar el vehículo"
                    )
                )
            )
        } catch (e: NoSuchElementException) {
            Result.failure(Exception(VEHICLE_NOT_FOUND))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar el vehículo"))
        }
    }

    private companion object {
        const val VEHICLE_NOT_FOUND = "Este vehículo no existe o fue eliminado"
    }
}
