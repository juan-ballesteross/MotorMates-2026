package com.example.motormates.data.repository

import com.example.motormates.data.dto.NotificationDto
import com.example.motormates.data.model.Notification
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.EventDataSource
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
        reviewId: String? = null,
        eventId: String? = null
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
    private val vehicleDataSource: VehicleRemoteDataSource,
    /**
     * La fuente de datos y no EventRepository: ese depende de este repositorio
     * para avisarle al organizador de un evento, y pedirle aquí lo contrario
     * cerraría un ciclo que Dagger rechaza.
     */
    private val eventDataSource: EventDataSource
) : NotificationRepository {

    override suspend fun notify(
        recipientUid: String,
        type: NotificationType,
        actorId: String,
        vehicleId: String?,
        reviewId: String?,
        eventId: String?
    ) {
        runCatching {
            notificationDataSource.createNotification(
                recipientUid, type.name, actorId, vehicleId, reviewId, eventId
            )
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
     * Se resuelven el actor, el vehículo y el título del evento de cada
     * notificación en paralelo (una lectura por id, igual que
     * getFollowers/getFollowing), no con un whereIn: así no hace falta crear
     * índices compuestos. Lo comparten la consulta puntual y la que escucha
     * en tiempo real.
     *
     * El título del evento se lee en vez de guardarse desnormalizado en el
     * documento de la notificación, al contrario que la copia del perfil en
     * "reviews" y "events": esa la repara la Cloud Function
     * updateUserInfoInReviews cuando el usuario se renombra, y no hay nada
     * equivalente que repare títulos, así que una copia quedaría congelada.
     */
    private suspend fun List<NotificationDto>.toDomain(): List<Notification> = coroutineScope {
        val actorIds = map { it.actorId }.distinct()
        val vehicleIds = mapNotNull { it.vehicleId }.distinct()
        val eventIds = mapNotNull { it.eventId }.distinct()

        val actorsById = actorIds
            .map { id -> async { id to runCatching { userDataSource.getUser(id) }.getOrNull() } }
            .map { it.await() }
            .toMap()

        val vehiclesById = vehicleIds
            .map { id -> async { id to runCatching { vehicleDataSource.getVehicleById(id).toDomain() }.getOrNull() } }
            .map { it.await() }
            .toMap()

        // getEventById lanza si el evento ya no existe (lo pudo cancelar y
        // borrar el organizador): la notificación se conserva sin título, no
        // se descarta la fila.
        val eventTitlesById = eventIds
            .map { id ->
                async { id to runCatching { eventDataSource.getEventById(id).title.ifBlank { null } }.getOrNull() }
            }
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
                eventId = dto.eventId,
                eventTitle = dto.eventId?.let { eventTitlesById[it] },
                createdAt = if (dto.createdAt > 0) Instant.ofEpochMilli(dto.createdAt).toString() else null
            )
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar las notificaciones"
    }
}
