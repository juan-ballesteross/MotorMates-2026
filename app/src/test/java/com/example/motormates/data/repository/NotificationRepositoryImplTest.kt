package com.example.motormates.data.repository

import com.example.motormates.data.dto.EventFirestoreDto
import com.example.motormates.data.dto.NotificationDto
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.EventDataSource
import com.example.motormates.data.source.NotificationDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * NotificationRepositoryImpl aislado con MockK. Lo que se prueba es la
 * hidratación: la lista de documentos llega plana (solo ids) y el repositorio
 * la completa con el actor, el vehículo y el título del evento leyendo una vez
 * por id distinto.
 */
class NotificationRepositoryImplTest {

    private val notificationDataSource = mockk<NotificationDataSource>(relaxed = true)
    private val userDataSource = mockk<UserDataSource>()
    private val vehicleDataSource = mockk<VehicleRemoteDataSource>()
    private val eventDataSource = mockk<EventDataSource>()

    private val repository = NotificationRepositoryImpl(
        notificationDataSource = notificationDataSource,
        userDataSource = userDataSource,
        vehicleDataSource = vehicleDataSource,
        eventDataSource = eventDataSource
    )

    /**
     * Dos avisos del mismo evento son UNA lectura: los ids se agrupan con
     * distinct antes de resolverlos. Importa porque un Car Meet con muchos
     * asistentes llena la campana de filas del mismo evento.
     */
    @Test
    fun `el titulo del evento se resuelve una sola vez por evento`() = runTest {
        coEvery { userDataSource.getUser(any()) } returns UserDto(fullName = "Beto Marca")
        coEvery { notificationDataSource.getNotifications("uid-ana") } returns listOf(
            notificacionDeEvento(id = "n1", actorId = "uid-beto"),
            notificacionDeEvento(id = "n2", actorId = "uid-carla")
        )
        coEvery { eventDataSource.getEventById("evt-1") } returns
            EventFirestoreDto(id = "evt-1", title = "Encuentro en el 93")

        val resultado = repository.getNotifications("uid-ana")

        assertThat(resultado.getOrNull()).hasSize(2)
        assertThat(resultado.getOrNull()?.map { it.eventTitle })
            .containsExactly("Encuentro en el 93", "Encuentro en el 93")
        coVerify(exactly = 1) { eventDataSource.getEventById("evt-1") }
    }

    /**
     * El organizador pudo borrar el evento. La fila se conserva sin título (el
     * mensaje dice "le interesa tu evento" a secas) en vez de desaparecer.
     */
    @Test
    fun `si el evento ya no existe la notificacion se conserva sin titulo`() = runTest {
        coEvery { userDataSource.getUser(any()) } returns UserDto(fullName = "Beto Marca")
        coEvery { notificationDataSource.getNotifications("uid-ana") } returns
            listOf(notificacionDeEvento())
        coEvery { eventDataSource.getEventById("evt-1") } throws
            NoSuchElementException("No existe el evento evt-1")

        val resultado = repository.getNotifications("uid-ana")

        assertThat(resultado.getOrNull()).hasSize(1)
        assertThat(resultado.getOrNull()?.first()?.eventTitle).isNull()
        assertThat(resultado.getOrNull()?.first()?.eventId).isEqualTo("evt-1")
    }

    /** Un tipo que esta versión de la app no conoce no puede tumbar la lista. */
    @Test
    fun `un tipo desconocido se descarta de la lista`() = runTest {
        coEvery { userDataSource.getUser(any()) } returns UserDto(fullName = "Beto Marca")
        coEvery { notificationDataSource.getNotifications("uid-ana") } returns listOf(
            notificacionDeEvento(id = "n1"),
            notificacionDeEvento(id = "n2").copy(type = "EVENT_RODADA", eventId = null)
        )
        coEvery { eventDataSource.getEventById("evt-1") } returns
            EventFirestoreDto(id = "evt-1", title = "Encuentro en el 93")

        val resultado = repository.getNotifications("uid-ana")

        assertThat(resultado.getOrNull()?.map { it.id }).containsExactly("n1")
    }

    @Test
    fun `eventId viaja hasta la fuente de datos`() = runTest {
        repository.notify(
            recipientUid = "uid-ana",
            type = NotificationType.EVENT_GOING,
            actorId = "uid-beto",
            eventId = "evt-1"
        )

        coVerify(exactly = 1) {
            notificationDataSource.createNotification(
                "uid-ana", "EVENT_GOING", "uid-beto", null, null, "evt-1"
            )
        }
    }

    /** Avisar es best-effort: un fallo al escribir no se propaga. */
    @Test
    fun `un fallo al avisar no se propaga`() = runTest {
        coEvery {
            notificationDataSource.createNotification(any(), any(), any(), any(), any(), any())
        } throws IOException("sin red")

        repository.notify(
            recipientUid = "uid-ana",
            type = NotificationType.EVENT_GOING,
            actorId = "uid-beto",
            eventId = "evt-1"
        )
    }

    // ---------- Fixtures ----------

    private fun notificacionDeEvento(
        id: String = "n1",
        actorId: String = "uid-beto",
        eventId: String = "evt-1"
    ) = NotificationDto(
        id = id,
        type = NotificationType.EVENT_INTERESTED.name,
        actorId = actorId,
        eventId = eventId,
        createdAt = 1_700_000_000_000L
    )
}
