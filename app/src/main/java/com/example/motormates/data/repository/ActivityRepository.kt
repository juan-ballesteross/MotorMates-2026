package com.example.motormates.data.repository

import com.example.motormates.data.model.ACTIVITY_LIMIT
import com.example.motormates.data.model.ActivityItem
import com.example.motormates.data.model.buildActivityTimeline
import com.example.motormates.data.source.ActivityDataSource
import com.example.motormates.data.source.AuthDataSource
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** La línea de tiempo de la pestaña Actividad del perfil propio. */
interface ActivityRepository {
    suspend fun getMyActivity(): Result<List<ActivityItem>>
}

class ActivityRepositoryImpl @Inject constructor(
    private val activityDataSource: ActivityDataSource,
    private val reviewRepository: ReviewRepository,
    private val vehicleRepository: VehicleRepository,
    private val authDataSource: AuthDataSource
) : ActivityRepository {

    /**
     * Las cinco fuentes se piden en paralelo y cada una es opcional: si una
     * falla (por ejemplo, los likes sin el índice de Firestore) la línea de
     * tiempo se arma con las demás. Solo es error si no se pudo leer nada.
     */
    override suspend fun getMyActivity(): Result<List<ActivityItem>> {
        val uid = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception("No se encontró tu sesión"))

        return coroutineScope {
            val reviews = async { reviewRepository.getReviewsByUser(uid) }
            val vehicles = async { vehicleRepository.getVehicles() }
            val likes = async { runCatching { activityDataSource.getReviewLikesBy(uid, ACTIVITY_LIMIT) } }
            val follows = async { runCatching { activityDataSource.getFollowingBy(uid) } }
            val stories = async { runCatching { activityDataSource.getStoriesBy(uid) } }

            val results = listOf(reviews.await(), vehicles.await(), likes.await(), follows.await(), stories.await())
            if (results.all { it.isFailure }) {
                return@coroutineScope Result.failure(Exception("No se pudo cargar tu actividad"))
            }

            Result.success(
                buildActivityTimeline(
                    myUid = uid,
                    reviews = reviews.await().getOrElse { emptyList() },
                    vehicles = vehicles.await().getOrElse { emptyList() },
                    likes = likes.await().getOrElse { emptyList() },
                    follows = follows.await().getOrElse { emptyList() },
                    stories = stories.await().getOrElse { emptyList() }
                )
            )
        }
    }
}
