package com.example.motormates.data.source

import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.VehicleDtoGeneric
import com.example.motormates.data.dto.VehicleFirestoreDto
import com.example.motormates.data.model.NewVehicle
import com.example.motormates.data.model.VehicleDraft
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Escribir vehículos nuevos. Va aparte de VehicleRemoteDataSource (que solo
 * lee) para no obligar a las fuentes de solo lectura, ni a los fakes de las
 * pruebas, a implementar algo que no usan.
 */
interface VehiclePublishDataSource {
    suspend fun createVehicle(vehicle: NewVehicle): VehicleDtoGeneric

    /** Cambia los datos y la foto (imageUrl null = sin foto). El dueño no cambia. */
    suspend fun updateVehicle(id: String, draft: VehicleDraft, imageUrl: String?)

    suspend fun deleteVehicle(id: String)
}

class FirestoreVehiclePublishDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : VehiclePublishDataSource {

    override suspend fun createVehicle(vehicle: NewVehicle): VehicleDtoGeneric {
        val ref = firestore.collection(VEHICLES_COLLECTION).document()
        val draft = vehicle.draft
        val dto = VehicleFirestoreDto(
            id = ref.id,
            brand = draft.brand,
            model = draft.model,
            year = draft.year,
            category = draft.category,
            imageUrl = vehicle.imageUrl,
            potencia = draft.potencia,
            aceleracion = draft.aceleracion,
            velocidadMaxima = draft.velocidadMaxima,
            traccion = draft.traccion,
            ownerId = vehicle.ownerId,
            // Copia del dueño, igual que el autor en las reseñas: el feed y el
            // detalle lo pintan sin leer su perfil.
            owner = ReviewUserSnapshot(fullName = vehicle.ownerName, avatarUrl = vehicle.ownerAvatarUrl),
            createdAt = System.currentTimeMillis()
        )
        ref.set(dto).await()
        return dto
    }

    override suspend fun updateVehicle(id: String, draft: VehicleDraft, imageUrl: String?) {
        // update() y no set(): no toca ownerId, owner ni createdAt. Las reglas
        // igual impiden cambiar de dueño.
        firestore.collection(VEHICLES_COLLECTION).document(id).update(
            mapOf(
                "brand" to draft.brand,
                "model" to draft.model,
                "year" to draft.year,
                "category" to draft.category,
                "potencia" to draft.potencia,
                "aceleracion" to draft.aceleracion,
                "velocidadMaxima" to draft.velocidadMaxima,
                "traccion" to draft.traccion,
                "imageUrl" to imageUrl,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun deleteVehicle(id: String) {
        firestore.collection(VEHICLES_COLLECTION).document(id).delete().await()
    }
}
