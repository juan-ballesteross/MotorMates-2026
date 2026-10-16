package com.example.motormates.data.source

import com.example.motormates.data.dto.VehicleDtoGeneric
import com.example.motormates.data.dto.VehicleFirestoreDto
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal const val VEHICLES_COLLECTION = "vehicles"

class FirestoreVehicleDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : VehicleRemoteDataSource {

    override suspend fun getVehicles(): List<VehicleDtoGeneric> {
        val snapshot = firestore.collection(VEHICLES_COLLECTION).get().await()
        return snapshot.toObjects(VehicleFirestoreDto::class.java)
    }

    override suspend fun getVehicleById(id: String): VehicleDtoGeneric {
        val snapshot = firestore.collection(VEHICLES_COLLECTION).document(id).get().await()
        return snapshot.toObject(VehicleFirestoreDto::class.java)
            ?: throw NoSuchElementException("No existe el vehículo $id")
    }
}
