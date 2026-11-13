package com.example.motormates.data.remote.service

import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.LikeRequest
import com.example.motormates.data.remote.dto.ReviewDto
import com.example.motormates.data.remote.dto.UpdateReviewRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Los dos primeros GET cuelgan de /vehicles y /users, pero devuelven
 * reseñas, así que viven en este servicio y no en los otros dos. Los ids
 * van como String: el dominio los maneja así (uid de Firebase incluido).
 */
interface ReviewApiService {

    @GET("vehicles/{id}/reviews")
    suspend fun getReviewsByVehicle(@Path("id") vehicleId: String): List<ReviewDto>

    @GET("users/{id}/reviews")
    suspend fun getReviewsByUser(@Path("id") userId: String): List<ReviewDto>

    /** Feed "Siguiendo": userIds separados por coma. */
    @GET("reviews")
    suspend fun getReviewsByUsers(@Query("userIds") userIds: String): List<ReviewDto>

    /**
     * Mapa de reseñas recientes: las publicadas desde `since` (epoch millis).
     *
     * OJO: el backend propio (Express + Postgres) vive en otro repositorio y
     * todavía no tiene ni las columnas de ubicación ni este parámetro, así que
     * hoy responde 404. Se declara igual para que las dos implementaciones de
     * ReviewRealtimeDataSource cumplan el mismo contrato; con
     * ACTIVE_BACKEND = FIREBASE (el valor actual) esto no se usa.
     */
    @GET("reviews")
    suspend fun getRecentReviews(@Query("since") since: Long): List<ReviewDto>

    @POST("reviews")
    suspend fun createReview(@Body body: CreateReviewRequest): ReviewDto

    @PUT("reviews/{id}")
    suspend fun updateReview(@Path("id") id: String, @Body body: UpdateReviewRequest): ReviewDto

    /**
     * El backend responde 204 sin cuerpo. Se declara sin tipo de retorno
     * (Unit) en vez de Response<Unit> para no acoplar las capas de arriba
     * al tipo Response de Retrofit: un código != 2xx lanza HttpException,
     * que es lo que atrapa el repositorio.
     */
    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String)

    @POST("reviews/{id}/likes")
    suspend fun likeReview(@Path("id") reviewId: String, @Body body: LikeRequest)

    @DELETE("reviews/{id}/likes/{userId}")
    suspend fun unlikeReview(@Path("id") reviewId: String, @Path("userId") userId: String)
}
