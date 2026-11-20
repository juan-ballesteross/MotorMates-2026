package com.example.motormates.data.repository

import android.net.Uri
import com.example.motormates.data.model.Event
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.MyEvent
import com.example.motormates.data.model.MyEventRole
import com.example.motormates.data.model.NewEvent
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.ParticipationChange
import com.example.motormates.data.model.ParticipationStatus
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.EventDataSource
import com.example.motormates.data.source.EventParticipationDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

/**
 * Los Car Meets y los piques.
 *
 * El organizador nunca llega desde la UI: se resuelve aquí con
 * [AuthDataSource], igual que el autor de una reseña. Así la pantalla no
 * puede publicar a nombre de otro ni por error. Lo mismo vale para el
 * destinatario de las notificaciones de participación: se lee del evento
 * (ver notifyOrganizer).
 */
interface EventRepository {

    /**
     * Los eventos cuyo inicio cae en la ventana dada. La ventana la decide
     * quien llama (es una decisión de producto, como el corte de 24 h de las
     * reseñas), así que el repositorio queda genérico.
     *
     * Los documentos mal formados se descartan al mapear, no se propagan.
     */
    fun observeEventsBetween(fromMillis: Long, toMillis: Long): Flow<Result<List<Event>>>

    suspend fun getEventById(eventId: String): Result<Event>

    suspend fun createEvent(draft: EventDraft): Result<Event>

    suspend fun updateEvent(eventId: String, draft: EventDraft): Result<Event>

    suspend fun cancelEvent(eventId: String): Result<Unit>

    fun observeMyParticipation(eventId: String): Flow<ParticipationStatus?>

    /** status null quita la participación. */
    suspend fun setParticipation(eventId: String, status: ParticipationStatus?): Result<Unit>

    /**
     * Los eventos que organizo o a los que dije "voy" / "me interesa", de
     * los últimos 90 días a los próximos 180, ordenados por fecha.
     */
    suspend fun getMyEvents(nowMillis: Long): Result<List<MyEvent>>
}

/**
 * Lo que la pantalla sí puede decidir de un evento. Es [NewEvent] sin los
 * datos del organizador: esos los pone el repositorio.
 */
data class EventDraft(
    val type: EventType,
    val title: String,
    val description: String?,
    val location: ReviewLocation,
    val venueName: String?,
    val address: String?,
    val startAt: Long,
    val endAt: Long? = null,
    val features: Set<EventFeature> = emptySet(),
    val imageUri: Uri? = null,
    val existingImageUrl: String? = null
)

class EventRepositoryImpl @Inject constructor(
    private val eventDataSource: EventDataSource,
    private val participationDataSource: EventParticipationDataSource,
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource,
    private val notificationRepository: NotificationRepository
) : EventRepository {

    override fun observeEventsBetween(
        fromMillis: Long,
        toMillis: Long
    ): Flow<Result<List<Event>>> =
        eventDataSource.observeEventsBetween(fromMillis, toMillis)
            .map { dtos -> Result.success(dtos.mapNotNull { it.toDomain() }) }
            .catch { emit(Result.failure(Exception(LOAD_FAILED))) }

    /**
     * Sin índices nuevos: se leen los eventos de la ventana (la misma
     * consulta del mapa) y, de cada uno, mi documento de participación, en
     * paralelo. Con los eventos de un proyecto de clase el costo es bajo.
     */
    override suspend fun getMyEvents(nowMillis: Long): Result<List<MyEvent>> {
        val uid = authDataSource.getCurrentUser()?.id
            ?: return Result.failure(Exception(NOT_SIGNED_IN_MY_EVENTS))
        return safeCall(notFound = LOAD_FAILED, fallback = LOAD_FAILED) {
            val events = eventDataSource.observeEventsBetween(nowMillis - MY_EVENTS_PAST_MS, nowMillis + MY_EVENTS_FUTURE_MS)
                .first()
                .mapNotNull { it.toDomain() }
            coroutineScope {
                events.map { event ->
                    async {
                        val role = if (event.organizerId == uid) {
                            MyEventRole.ORGANIZER
                        } else {
                            when (runCatching { participationDataSource.getParticipation(event.id, uid) }.getOrNull()) {
                                ParticipationStatus.GOING -> MyEventRole.GOING
                                ParticipationStatus.INTERESTED -> MyEventRole.INTERESTED
                                null -> null
                            }
                        }
                        role?.let { MyEvent(event, it) }
                    }
                }.awaitAll().filterNotNull().sortedBy { it.event.startAt }
            }
        }
    }

    override suspend fun getEventById(eventId: String): Result<Event> =
        safeCall(notFound = EVENT_NOT_FOUND, fallback = LOAD_FAILED) {
            eventDataSource.getEventById(eventId).toDomain()
                ?: throw NoSuchElementException(EVENT_NOT_FOUND)
        }

    override suspend fun createEvent(draft: EventDraft): Result<Event> =
        safeCall(notFound = EVENT_NOT_FOUND, fallback = SAVE_FAILED) {
            val newEvent = draft.withOrganizer()
            eventDataSource.createEvent(newEvent).toDomain()
                ?: throw IllegalStateException(SAVE_FAILED)
        }

    override suspend fun updateEvent(eventId: String, draft: EventDraft): Result<Event> =
        safeCall(notFound = EVENT_NOT_FOUND, fallback = SAVE_FAILED) {
            requireOrganizer(eventId)
            eventDataSource.updateEvent(eventId, draft.withOrganizer()).toDomain()
                ?: throw IllegalStateException(SAVE_FAILED)
        }

    override suspend fun cancelEvent(eventId: String): Result<Unit> =
        safeCall(notFound = EVENT_NOT_FOUND, fallback = "No se pudo cancelar el evento") {
            requireOrganizer(eventId)
            eventDataSource.cancelEvent(eventId)
        }

    /**
     * Sin sesión no hay participación que mirar, y el flujo emite null en vez
     * de fallar: es un adorno de la tarjeta, no debe tumbarla.
     */
    override fun observeMyParticipation(eventId: String): Flow<ParticipationStatus?> {
        val userId = authDataSource.getCurrentUser()?.id ?: return flowOf(null)
        return participationDataSource.observeMyParticipation(eventId, userId)
            .catch { emit(null) }
    }

    override suspend fun setParticipation(
        eventId: String,
        status: ParticipationStatus?
    ): Result<Unit> =
        safeCall(notFound = EVENT_NOT_FOUND, fallback = "No se pudo guardar tu respuesta") {
            val userId = currentUserId()
            val change = participationDataSource.setParticipation(eventId, userId, status)
            // Solo la marca nueva avisa: pasar de "me interesa" a "voy" y
            // quitar la participación no le interesan al organizador, y
            // volver a marcar lo mismo no escribió nada.
            if (change == ParticipationChange.CREATED && status != null) {
                notifyOrganizer(eventId, userId, status)
            }
        }

    /**
     * Secundario y best-effort, igual que el notify de FollowRepositoryImpl:
     * si falla, la participación ya quedó guardada y la pantalla no se entera.
     *
     * El runCatching es propio y no basta el de NotificationRepository.notify
     * porque este bloque corre DENTRO del safeCall: sin él, un fallo leyendo
     * el evento convertiría una participación ya escrita en Result.failure y
     * el ViewModel revertiría el botón contra la verdad del servidor. En
     * likeReview no hace falta porque allí notify va fuera del safeLikeCall.
     *
     * El organizador se lee del evento y no llega por la firma, al contrario
     * que reviewAuthorId en likeReview: firestore.rules deja crear
     * notificaciones en la bandeja de cualquier uid con solo firmar el propio
     * actorId, así que el destinatario no debe poder elegirlo la pantalla.
     *
     * Marcar, quitar y volver a marcar sí avisa de nuevo, igual que seguir,
     * dejar de seguir y volver a seguir.
     */
    private suspend fun notifyOrganizer(
        eventId: String,
        actorId: String,
        status: ParticipationStatus
    ) {
        runCatching {
            notificationRepository.notify(
                recipientUid = eventDataSource.getEventById(eventId).userId,
                type = status.toNotificationType(),
                actorId = actorId,
                eventId = eventId
            )
        }
    }

    /**
     * Se comprueba también en el cliente, aunque firestore.rules ya lo
     * refuerce: así el usuario ve un mensaje claro en vez de un
     * PERMISSION_DENIED traducido a genérico.
     */
    private suspend fun requireOrganizer(eventId: String) {
        val userId = currentUserId()
        val event = eventDataSource.getEventById(eventId)
        if (event.userId != userId) throw IllegalStateException(NOT_ORGANIZER)
    }

    private fun currentUserId(): String =
        authDataSource.getCurrentUser()?.id ?: throw IllegalStateException(NOT_SIGNED_IN)

    /** Añade la captura desnormalizada del organizador, como hace createReview. */
    private suspend fun EventDraft.withOrganizer(): NewEvent {
        val currentUser = authDataSource.getCurrentUser()
            ?: throw IllegalStateException(NOT_SIGNED_IN)
        val profile = runCatching { userDataSource.getUser(currentUser.id) }.getOrNull()

        return NewEvent(
            type = type,
            title = title.trim(),
            description = description?.trim()?.ifBlank { null },
            organizerId = currentUser.id,
            organizerName = profile?.fullName?.ifBlank { null }
                ?: currentUser.displayName?.ifBlank { null }
                ?: ORGANIZER_FALLBACK_NAME,
            organizerAvatarUrl = profile?.avatarUrl ?: currentUser.photoUrl,
            location = location,
            venueName = venueName?.trim()?.ifBlank { null },
            address = address?.trim()?.ifBlank { null },
            startAt = startAt,
            endAt = endAt,
            features = features,
            imageUri = imageUri,
            existingImageUrl = existingImageUrl
        )
    }

    /** Mismo orden de catches que ReviewRepositoryImpl. */
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
            Result.failure(Exception(e.toSpanishMessage(notFound = notFound, fallback = fallback)))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toSpanishMessage(notFound = notFound, fallback = fallback)))
        } catch (e: NoSuchElementException) {
            Result.failure(Exception(notFound))
        } catch (e: IllegalStateException) {
            // Los errores que este repositorio lanza a propósito (sin sesión,
            // no es el organizador) ya traen el mensaje listo para la UI.
            Result.failure(Exception(e.message ?: fallback))
        } catch (e: Exception) {
            Result.failure(Exception(fallback))
        }
    }

    private companion object {
        const val EVENT_NOT_FOUND = "Este evento ya no existe"
        const val LOAD_FAILED = "No se pudieron cargar los eventos"
        const val NOT_SIGNED_IN_MY_EVENTS = "Debes iniciar sesión para ver tus eventos"
        private const val DAY_MS = 24L * 60 * 60 * 1000
        const val MY_EVENTS_PAST_MS = 90 * DAY_MS
        const val MY_EVENTS_FUTURE_MS = 180 * DAY_MS
        const val SAVE_FAILED = "No se pudo guardar el evento"
        const val NOT_SIGNED_IN = "Debes iniciar sesión para publicar un evento"
        const val NOT_ORGANIZER = "Solo el organizador puede editar este evento"
        const val ORGANIZER_FALLBACK_NAME = "Usuario"
    }
}

/**
 * Vive en el repositorio y no en la fuente de datos ni en el enum: qué se le
 * avisa al organizador es una regla de negocio, igual que la de "solo la marca
 * nueva avisa". La fuente de datos solo sabe de contadores y documentos.
 */
private fun ParticipationStatus.toNotificationType(): NotificationType = when (this) {
    ParticipationStatus.INTERESTED -> NotificationType.EVENT_INTERESTED
    ParticipationStatus.GOING -> NotificationType.EVENT_GOING
}
