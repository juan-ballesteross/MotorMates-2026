package com.example.motormates.data.repository

import com.example.motormates.data.dto.NotificationDto
import com.example.motormates.data.model.Notification
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.NotificationDataSource
import com.example.motormates.data.source.UserDataSource
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface NotificationRepository {
    suspend fun notify(
        recipientUid: String,
        type: NotificationType,
        actorId: String,
        vehicleId: String? = null,
        reviewId: String? = null
    )

    suspend fun getNotifications(uid: String): Result<List<Notification>>

    /**
     * En tiempo real: el like que alguien le da a mi reseña aparece en la
     * campana sin tener que volver a entrar a la pantalla.
     */
    fun observeNotifications(uid: String): Flow<Result<List<Notification>>>
}

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDataSource: NotificationDataSource,
    private val userDataSource: UserDataSource,
    private val vehicleDataSource: VehicleRemoteDataSource
) : NotificationRepository {

    override suspend fun notify(
        recipientUid: String,
        type: NotificationType,
        actorId: String,
        vehicleId: String?,
        reviewId: String?
    ) {
        runCatching {
            notificationDataSource.createNotification(recipientUid, type.name, actorId, vehicleId, reviewId)
        }
    }

    override suspend fun getNotifications(uid: String): Result<List<Notification>> {
        return try {
            Result.success(notificationDataSource.getNotifications(uid).toDomain())
        } catch (e: Exception) {
            Result.failure(Exception(LOAD_ERROR))
        }
    }

    override fun observeNotifications(uid: String): Flow<Result<List<Notification>>> =
        notificationDataSource.observeNotifications(uid)
            .map { dtos -> Result.success(dtos.toDomain()) }
            .catch { emit(Result.failure(Exception(LOAD_ERROR))) }

    /**
     * Se resuelven el actor y el vehículo de cada notificación en paralelo
     * (una lectura por id, igual que getFollowers/getFollowing), no con un
     * whereIn: así no hace falta crear índices compuestos. Lo comparten la
     * consulta puntual y la que escucha en tiempo real.
     */
    private suspend fun List<NotificationDto>.toDomain(): List<Notification> = coroutineScope {
        val actorIds = map { it.actorId }.distinct()
        val vehicleIds = mapNotNull { it.vehicleId }.distinct()

        val actorsById = actorIds
            .map { id -> async { id to runCatching { userDataSource.getUser(id) }.getOrNull() } }
            .map { it.await() }
            .toMap()

        val vehiclesById = vehicleIds
            .map { id -> async { id to runCatching { vehicleDataSource.getVehicleById(id).toDomain() }.getOrNull() } }
            .map { it.await() }
            .toMap()

        mapNotNull { dto ->
            val type = runCatching { NotificationType.valueOf(dto.type) }.getOrNull()
                ?: return@mapNotNull null
            val actor = actorsById[dto.actorId]
            val vehicle = dto.vehicleId?.let { vehiclesById[it] }

            Notification(
                id = dto.id,
                type = type,
                actorId = dto.actorId,
                actorName = actor?.fullName?.ifBlank { null } ?: "Usuario",
                actorAvatarUrl = actor?.avatarUrl,
                vehicleId = dto.vehicleId,
                vehicleBrand = vehicle?.brand,
                vehicleModel = vehicle?.model,
                vehicleImageUrl = vehicle?.imageUrl,
                reviewId = dto.reviewId,
                createdAt = if (dto.createdAt > 0) Instant.ofEpochMilli(dto.createdAt).toString() else null
            )
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar las notificaciones"
    }
}
