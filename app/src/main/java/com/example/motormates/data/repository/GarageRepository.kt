package com.example.motormates.data.repository

import android.net.Uri
import com.example.motormates.data.model.NewVehicle
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.UserDataSource
import com.example.motormates.data.source.VehicleImageDataSource
import com.example.motormates.data.source.VehiclePublishDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

/** El garaje de cada cuenta: publicar, editar y eliminar vehículos propios. */
interface GarageRepository {
    /**
     * El dueño es siempre el usuario autenticado: no se recibe por parámetro,
     * igual que el autor de una reseña. imageUri es opcional.
     */
    suspend fun publishVehicle(draft: VehicleDraft, imageUri: Uri?): Result<Vehicle>

    /** Un vehículo para editarlo. Falla si no existe o no es del usuario. */
    suspend fun getMyVehicle(vehicleId: String): Result<Vehicle>

    /**
     * Guarda los cambios. Foto: si llega newImageUri se sube y reemplaza a la
     * anterior; si no, se deja keepImageUrl (null = quitar la foto).
     */
    suspend fun updateVehicle(
        vehicleId: String,
        draft: VehicleDraft,
        newImageUri: Uri?,
        keepImageUrl: String?
    ): Result<Unit>

    suspend fun deleteVehicle(vehicleId: String): Result<Unit>
}

class GarageRepositoryImpl @Inject constructor(
    private val publishDataSource: VehiclePublishDataSource,
    private val imageDataSource: VehicleImageDataSource,
    private val vehicleDataSource: VehicleRemoteDataSource,
    private val userDataSource: UserDataSource,
    private val authDataSource: AuthDataSource
) : GarageRepository {

    override suspend fun publishVehicle(draft: VehicleDraft, imageUri: Uri?): Result<Vehicle> {
        val currentUser = authDataSource.getCurrentUser()
            ?: return Result.failure(Exception(NO_SESSION))

        return safeCall(PUBLISH_FAILED) {
            val profile = userDataSource.getUser(currentUser.id)
            // La foto primero: si falla no queda un vehículo a medias sin foto.
            val imageUrl = imageUri?.let { imageDataSource.uploadVehicleImage(currentUser.id, it) }

            publishDataSource.createVehicle(
                NewVehicle(
                    draft = draft,
                    ownerId = currentUser.id,
                    ownerName = profile?.fullName?.ifBlank { null }
                        ?: currentUser.displayName
                        ?: "Usuario",
                    ownerAvatarUrl = profile?.avatarUrl ?: currentUser.photoUrl,
                    imageUrl = imageUrl
                )
            ).toDomain()
        }
    }

    override suspend fun getMyVehicle(vehicleId: String): Result<Vehicle> {
        val currentUser = authDataSource.getCurrentUser()
            ?: return Result.failure(Exception(NO_SESSION))

        val result = safeCall(LOAD_FAILED) { vehicleDataSource.getVehicleById(vehicleId).toDomain() }
        val vehicle = result.getOrElse { return result }
        // La pantalla solo ofrece editar los propios, pero se verifica igual:
        // el id llega por la ruta de navegación.
        return if (vehicle.ownerId == currentUser.id) {
            Result.success(vehicle)
        } else {
            Result.failure(Exception(NOT_OWNER))
        }
    }

    override suspend fun updateVehicle(
        vehicleId: String,
        draft: VehicleDraft,
        newImageUri: Uri?,
        keepImageUrl: String?
    ): Result<Unit> {
        val currentUser = authDataSource.getCurrentUser()
            ?: return Result.failure(Exception(NO_SESSION))

        return safeCall(UPDATE_FAILED) {
            val imageUrl = newImageUri
                ?.let { imageDataSource.uploadVehicleImage(currentUser.id, it) }
                ?: keepImageUrl
            publishDataSource.updateVehicle(vehicleId, draft, imageUrl)
        }
    }

    override suspend fun deleteVehicle(vehicleId: String): Result<Unit> {
        if (authDataSource.getCurrentUser() == null) return Result.failure(Exception(NO_SESSION))
        return safeCall(DELETE_FAILED) { publishDataSource.deleteVehicle(vehicleId) }
    }

    /** Mismo manejo de errores para todas las operaciones, con el mensaje de cada una. */
    private suspend fun <T> safeCall(fallback: String, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: StorageException) {
            Result.failure(Exception("No se pudo subir la foto del vehículo"))
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(Exception(e.toSpanishMessage(notFound = NOT_FOUND, fallback = fallback)))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toSpanishMessage(notFound = NOT_FOUND, fallback = fallback)))
        } catch (e: NoSuchElementException) {
            Result.failure(Exception(NOT_FOUND))
        } catch (e: Exception) {
            Result.failure(Exception(fallback))
        }

    private companion object {
        const val NO_SESSION = "Debes iniciar sesión para publicar un vehículo"
        const val PUBLISH_FAILED = "No se pudo publicar el vehículo"
        const val LOAD_FAILED = "No se pudo cargar el vehículo"
        const val UPDATE_FAILED = "No se pudieron guardar los cambios"
        const val DELETE_FAILED = "No se pudo eliminar el vehículo"
        const val NOT_FOUND = "Este vehículo no existe o fue eliminado"
        const val NOT_OWNER = "Solo el dueño puede editar este vehículo"
    }
}
