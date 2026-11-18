package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.VehicleDtoGeneric
import com.example.motormates.data.model.NewVehicle
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.remote.dto.CreateVehicleRequest
import com.example.motormates.data.remote.dto.UpdateVehicleRequest
import com.example.motormates.data.remote.service.VehicleApiService
import com.example.motormates.data.source.VehiclePublishDataSource
import javax.inject.Inject

/** POST, PUT y DELETE /vehicles en el backend Express. La foto ya llega subida a Storage. */
class VehicleRetrofitPublishDataSource @Inject constructor(
    private val service: VehicleApiService
) : VehiclePublishDataSource {

    override suspend fun createVehicle(vehicle: NewVehicle): VehicleDtoGeneric {
        val draft = vehicle.draft
        return service.createVehicle(
            CreateVehicleRequest(
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
                ownerName = vehicle.ownerName,
                ownerAvatarUrl = vehicle.ownerAvatarUrl
            )
        )
    }

    override suspend fun updateVehicle(id: String, draft: VehicleDraft, imageUrl: String?) {
        service.updateVehicle(
            id,
            UpdateVehicleRequest(
                brand = draft.brand,
                model = draft.model,
                year = draft.year,
                category = draft.category,
                imageUrl = imageUrl,
                potencia = draft.potencia,
                aceleracion = draft.aceleracion,
                velocidadMaxima = draft.velocidadMaxima,
                traccion = draft.traccion
            )
        )
    }

    override suspend fun deleteVehicle(id: String) {
        service.deleteVehicle(id)
    }
}
