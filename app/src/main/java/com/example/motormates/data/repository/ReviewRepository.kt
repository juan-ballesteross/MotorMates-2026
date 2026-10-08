package com.example.motormates.data.repository

import com.example.motormates.data.model.Review
import com.example.motormates.data.remote.ReviewRemoteDataSource
import com.example.motormates.data.remote.dto.CreateReviewRequest
import com.example.motormates.data.remote.dto.UpdateReviewRequest
import com.example.motormates.data.remote.mapper.toDomain
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

interface ReviewRepository {
    suspend fun getReviewsByVehicle(vehicleId: Int): Result<List<Review>>
    suspend fun getReviewsByUser(userId: Int): Result<List<Review>>
    suspend fun createReview(
        userId: Int,
        vehicleId: Int,
        rating: Int,
        comment: String?
    ): Result<Review>
    suspend fun updateReview(reviewId: Int, rating: Int?, comment: String?): Result<Review>
    suspend fun deleteReview(reviewId: Int): Result<Unit>
}

class ReviewRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReviewRemoteDataSource
) : ReviewRepository {

    override suspend fun getReviewsByVehicle(vehicleId: Int): Result<List<Review>> {
        return try {
            Result.success(remoteDataSource.getReviewsByVehicle(vehicleId).map { it.toDomain() })
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Este vehículo ya no existe",
                        fallback = "No se pudieron cargar las reseñas"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudieron cargar las reseñas"))
        }
    }

    override suspend fun getReviewsByUser(userId: Int): Result<List<Review>> {
        return try {
            Result.success(remoteDataSource.getReviewsByUser(userId).map { it.toDomain() })
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Este usuario no existe",
                        fallback = "No se pudieron cargar las reseñas"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudieron cargar las reseñas"))
        }
    }

    override suspend fun createReview(
        userId: Int,
        vehicleId: Int,
        rating: Int,
        comment: String?
    ): Result<Review> {
        return try {
            val created = remoteDataSource.createReview(
                CreateReviewRequest(
                    userId = userId,
                    vehicleId = vehicleId,
                    rating = rating,
                    comment = comment
                )
            )
            Result.success(created.toDomain())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    if (e.code() == 400) {
                        "La calificación debe ser un número entre 0 y 5"
                    } else {
                        e.toSpanishMessage(
                            notFound = "El usuario o el vehículo ya no existe",
                            fallback = "No se pudo publicar la reseña"
                        )
                    }
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo publicar la reseña"))
        }
    }

    override suspend fun updateReview(
        reviewId: Int,
        rating: Int?,
        comment: String?
    ): Result<Review> {
        return try {
            val updated = remoteDataSource.updateReview(
                reviewId,
                UpdateReviewRequest(rating = rating, comment = comment)
            )
            Result.success(updated.toDomain())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    if (e.code() == 400) {
                        "La calificación debe ser un número entre 0 y 5"
                    } else {
                        e.toSpanishMessage(
                            notFound = "Esta reseña ya no existe",
                            fallback = "No se pudo guardar la reseña"
                        )
                    }
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo guardar la reseña"))
        }
    }

    override suspend fun deleteReview(reviewId: Int): Result<Unit> {
        return try {
            Result.success(remoteDataSource.deleteReview(reviewId))
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Esta reseña ya no existe",
                        fallback = "No se pudo eliminar la reseña"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo eliminar la reseña"))
        }
    }
}
