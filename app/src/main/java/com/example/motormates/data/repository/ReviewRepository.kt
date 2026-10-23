package com.example.motormates.data.repository

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.model.Review
import com.example.motormates.data.remote.datasource.ReviewRemoteDataSource
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.ReviewRealtimeDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

interface ReviewRepository {
    suspend fun getReviewsByVehicle(vehicleId: String): Result<List<Review>>
    suspend fun getReviewsByUser(userId: String): Result<List<Review>>

    /**
     * Las dos consultas en tiempo real: emiten de nuevo cada vez que alguien
     * publica, edita o borra una reseña, o cuando cambia su likesCount.
     */
    fun observeReviewsByVehicle(vehicleId: String): Flow<Result<List<Review>>>
    fun observeReviewsByUsers(userIds: List<String>): Flow<Result<List<Review>>>

    /** El autor es siempre el usuario autenticado: no se recibe por parámetro. */
    suspend fun createReview(vehicleId: String, rating: Int, comment: String?): Result<Review>
    suspend fun updateReview(reviewId: String, rating: Int?, comment: String?): Result<Review>
    suspend fun deleteReview(reviewId: String): Result<Unit>
}

/**
 * Orquesta varias fuentes de datos, como el TweetRepository de la guía:
 * para crear una reseña necesita el uid de Auth, el perfil del autor
 * (Firestore "users") y el vehículo, porque nombre/foto del autor y
 * marca/modelo del vehículo se guardan desnormalizados dentro de la reseña.
 */
class ReviewRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReviewRemoteDataSource,
    private val realtimeDataSource: ReviewRealtimeDataSource,
    private val vehicleDataSource: VehicleRemoteDataSource,
    private val userDataSource: UserDataSource,
    private val authDataSource: AuthDataSource
) : ReviewRepository {

    override suspend fun getReviewsByVehicle(vehicleId: String): Result<List<Review>> =
        safeCall(notFound = "Este vehículo ya no existe", fallback = "No se pudieron cargar las reseñas") {
            remoteDataSource.getReviewsByVehicle(vehicleId).map { it.toDomain() }
        }

    override suspend fun getReviewsByUser(userId: String): Result<List<Review>> =
        safeCall(notFound = "Este usuario no existe", fallback = "No se pudieron cargar las reseñas") {
            remoteDataSource.getReviewsByUser(userId).map { it.toDomain() }
        }

    override fun observeReviewsByVehicle(vehicleId: String): Flow<Result<List<Review>>> =
        realtimeDataSource.observeReviewsByVehicle(vehicleId).toDomainResults()

    override fun observeReviewsByUsers(userIds: List<String>): Flow<Result<List<Review>>> =
        realtimeDataSource.observeReviewsByUsers(userIds).toDomainResults()

    /**
     * safeCall no sirve aquí porque es suspend y envuelve una sola llamada:
     * un Flow puede fallar en cualquier emisión, así que el error se atrapa
     * con catch y se emite como Result.failure, igual que en el resto.
     */
    private fun Flow<List<ReviewDtoGeneric>>.toDomainResults(): Flow<Result<List<Review>>> =
        map { dtos -> Result.success(dtos.map { it.toDomain() }) }
            .catch { emit(Result.failure(Exception("No se pudieron cargar las reseñas"))) }

    override suspend fun createReview(
        vehicleId: String,
        rating: Int,
        comment: String?
    ): Result<Review> {
        val currentUser = authDataSource.getCurrentUser()
            ?: return Result.failure(Exception("Debes iniciar sesión para publicar una reseña"))
        if (rating !in 0..5) {
            return Result.failure(Exception(INVALID_RATING))
        }

        return safeCall(
            notFound = "El usuario o el vehículo ya no existe",
            fallback = "No se pudo publicar la reseña"
        ) {
            val profile = userDataSource.getUser(currentUser.id)
            val vehicle = vehicleDataSource.getVehicleById(vehicleId).toDomain()

            remoteDataSource.createReview(
                NewReview(
                    userId = currentUser.id,
                    vehicleId = vehicleId,
                    rating = rating,
                    comment = comment,
                    authorName = profile?.fullName?.ifBlank { null }
                        ?: currentUser.displayName
                        ?: "Usuario",
                    authorAvatarUrl = profile?.avatarUrl ?: currentUser.photoUrl,
                    vehicleBrand = vehicle.brand,
                    vehicleModel = vehicle.model
                )
            ).toDomain()
        }
    }

    override suspend fun updateReview(
        reviewId: String,
        rating: Int?,
        comment: String?
    ): Result<Review> {
        if (rating != null && rating !in 0..5) {
            return Result.failure(Exception(INVALID_RATING))
        }
        return safeCall(notFound = REVIEW_NOT_FOUND, fallback = "No se pudo guardar la reseña") {
            remoteDataSource.updateReview(reviewId, rating, comment).toDomain()
        }
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> =
        safeCall(notFound = REVIEW_NOT_FOUND, fallback = "No se pudo eliminar la reseña") {
            remoteDataSource.deleteReview(reviewId)
        }

    /**
     * Mismo orden de catches que el resto de repositorios: errores de
     * Retrofit, de Firestore, "no existe" y por último el genérico. Se
     * centraliza aquí porque las cinco operaciones solo cambian de mensaje.
     */
    private suspend fun <T> safeCall(
        notFound: String,
        fallback: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    if (e.code() == 400) INVALID_RATING
                    else e.toSpanishMessage(notFound = notFound, fallback = fallback)
                )
            )
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toSpanishMessage(notFound = notFound, fallback = fallback)))
        } catch (e: NoSuchElementException) {
            Result.failure(Exception(notFound))
        } catch (e: Exception) {
            Result.failure(Exception(fallback))
        }
    }

    private companion object {
        const val INVALID_RATING = "La calificación debe ser un número entre 0 y 5"
        const val REVIEW_NOT_FOUND = "Esta reseña ya no existe"
    }
}
