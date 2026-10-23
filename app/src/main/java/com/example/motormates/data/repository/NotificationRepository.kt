package com.example.motormates.data.repository

import com.example.motormates.data.model.Notification
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.NotificationDataSource
import com.example.motormates.data.source.UserDataSource
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface NotificationRepository {
    suspend fun notify(
        recipientUid: String,
        type: NotificationType,
        actorId: String,
        vehicleId: String? = null,
        reviewId: String? = null
    )

    suspend fun getNotifications(uid: String): Result<List<Notification>>
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
            val dtos = notificationDataSource.getNotifications(uid)
            val actorIds = dtos.map { it.actorId }.distinct()
            val vehicleIds = dtos.mapNotNull { it.vehicleId }.distinct()

            coroutineScope {
                val actorsById = actorIds
                    .map { id -> async { id to runCatching { userDataSource.getUser(id) }.getOrNull() } }
                    .map { it.await() }
                    .toMap()

                val vehiclesById = vehicleIds
                    .map { id -> async { id to runCatching { vehicleDataSource.getVehicleById(id).toDomain() }.getOrNull() } }
                    .map { it.await() }
                    .toMap()

                val notifications = dtos.mapNotNull { dto ->
                    val type = runCatching { NotificationType.valueOf(dto.type) }.getOrNull() ?: return@mapNotNull null
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

                Result.success(notifications)
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudieron cargar las notificaciones"))
        }
    }
}
