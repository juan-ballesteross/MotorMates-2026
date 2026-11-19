package com.example.motormates.data.remote.service

import com.example.motormates.data.remote.dto.SearchNearbyRequest
import com.example.motormates.data.remote.dto.SearchNearbyResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** La raíz de Places API (New). No tiene nada que ver con BASE_URL, que es el backend propio. */
const val PLACES_BASE_URL = "https://places.googleapis.com/"

/**
 * Places API (New).
 *
 * La llave y el field mask van como headers y no dentro del cuerpo porque así
 * lo exige la API. Los dos entran por parámetro, no con @Headers: el valor de
 * la llave sale de BuildConfig en tiempo de ejecución, y las anotaciones solo
 * admiten constantes.
 *
 * Nearby Search (New) existe solo como POST; no hay variante GET.
 */
interface PlacesApiService {

    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body request: SearchNearbyRequest
    ): SearchNearbyResponse
}
