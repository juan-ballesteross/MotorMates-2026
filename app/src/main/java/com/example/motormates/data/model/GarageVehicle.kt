package com.example.motormates.data.model

/** Un carro del garaje de un usuario, como se pinta en su perfil público. */
data class GarageVehicleUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    /** Specs ya con unidad ("181 hp"); null si no se llenaron. */
    val potencia: String? = null,
    val aceleracion: String? = null
)

/** Los carros que tiene a su nombre el usuario uid, ordenados por marca y modelo. */
fun List<Vehicle>.garageOf(uid: String): List<GarageVehicleUi> =
    filter { it.ownerId == uid }
        .sortedWith(compareBy({ it.brand.lowercase() }, { it.model.lowercase() }))
        .map { vehicle ->
            GarageVehicleUi(
                id = vehicle.id,
                title = "${vehicle.brand} ${vehicle.model}",
                subtitle = "${vehicle.year} · ${vehicle.category}",
                imageUrl = vehicle.imageUrl,
                potencia = vehicle.potencia,
                aceleracion = vehicle.aceleracion
            )
        }
