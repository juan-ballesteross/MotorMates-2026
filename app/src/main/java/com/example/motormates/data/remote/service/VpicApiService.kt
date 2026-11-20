package com.example.motormates.data.remote.service

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * vPIC, la base pública de vehículos de la NHTSA (EE. UU.). Es gratis y no
 * pide llave. Cubre bien Mazda, Chevrolet, Kia, Toyota, Hyundai, Nissan…
 * pero no Renault ni los autos de BYD, por eso CarCatalogRepository la
 * combina con una lista local del mercado colombiano.
 */
interface VpicApiService {

    /** vehicleType: "car", "truck" o "mpv" (multipropósito: SUV y camionetas). */
    @GET("GetModelsForMakeYear/make/{make}/modelyear/{year}/vehicletype/{vehicleType}?format=json")
    suspend fun getModels(
        @Path("make") make: String,
        @Path("year") year: Int,
        @Path("vehicleType") vehicleType: String
    ): VpicModelsResponse
}

const val VPIC_BASE_URL = "https://vpic.nhtsa.dot.gov/api/vehicles/"

data class VpicModelsResponse(
    @SerializedName("Count") val count: Int = 0,
    @SerializedName("Results") val results: List<VpicModel> = emptyList()
)

data class VpicModel(
    @SerializedName("Make_Name") val makeName: String? = null,
    @SerializedName("Model_Name") val modelName: String? = null
)
