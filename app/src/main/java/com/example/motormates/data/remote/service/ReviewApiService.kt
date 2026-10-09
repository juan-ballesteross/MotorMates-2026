package com.example.motormates.data.remote.service

import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.ReviewDto
import com.example.motormates.data.remote.dto.UpdateReviewRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Los dos primeros GET cuelgan de /vehicles y /users, pero devuelven
 * reseñas, así que viven en este servicio y no en los otros dos.
 */
interface ReviewApiService {

    @GET("vehicles/{id}/reviews")
    suspend fun getReviewsByVehicle(@Path("id") vehicleId: Int): List<ReviewDto>

    @GET("users/{id}/reviews")
    suspend fun getReviewsByUser(@Path("id") userId: Int): List<ReviewDto>

    @POST("reviews")
    suspend fun createReview(@Body body: CreateReviewRequest): ReviewDto

    @PUT("reviews/{id}")
    suspend fun updateReview(@Path("id") id: Int, @Body body: UpdateReviewRequest): ReviewDto

    /**
     * El backend responde 204 sin cuerpo. Se declara sin tipo de retorno
     * (Unit) en vez de Response<Unit> para no acoplar las capas de arriba
     * al tipo Response de Retrofit: un código != 2xx lanza HttpException,
     * que es lo que atrapa el repositorio.
     */
    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") id: Int)
}
