package com.example.motormates.data.repository

import com.example.motormates.data.model.MyEventRole
import com.example.motormates.data.dto.EventFirestoreDto
import com.example.motormates.data.dto.ReviewLocationSnapshot
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.NewEvent
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.ParticipationChange
import com.example.motormates.data.model.ParticipationStatus
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.EventDataSource
import com.example.motormates.data.source.EventParticipationDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * EventRepositoryImpl aislado con MockK: las fuentes responden lo que se les
 * configura y lo único que se ejecuta de verdad es la lógica del repositorio
 * (quién es el organizador, los mapeos, Result y los mensajes en español).
 */
class EventRepositoryImplTest {

    private val eventDataSource = mockk<EventDataSource>()
    private val participationDataSource = mockk<EventParticipationDataSource>(relaxed = true)
    private val authDataSource = mockk<AuthDataSource>()
    private val userDataSource = mockk<UserDataSource>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)

    private val repository = EventRepositoryImpl(
        eventDataSource = eventDataSource,
        participationDataSource = participationDataSource,
        authDataSource = authDataSource,
        userDataSource = userDataSource,
        notificationRepository = notificationRepository
    )

    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643, placeName = "Chapinero")
    private val inicio = 1_700_000_000_000L
    private val ana = AuthUser(id = "uid-ana", email = "ana@mail.com", displayName = "Ana")

    /** Quien marca. Los eventos de las fixtures los organiza Ana. */
    private val beto = AuthUser(id = "uid-beto", email = "beto@mail.com", displayName = "Beto")

    // ---------- Organizador ----------

    @Test
    fun `sin sesion no se puede crear un evento`() = runTest {
        // Arrange
        every { authDataSource.getCurrentUser() } returns null

        // Act
        val resultado = repository.createEvent(draft())

        // Assert
        assertThat(resultado.isFailure).isTrue()
        assertThat(resultado.exceptionOrNull()?.message)
            .isEqualTo("Debes iniciar sesión para publicar un evento")
        coVerify(exactly = 0) { eventDataSource.createEvent(any()) }
    }

    /**
     * El organizador no llega desde la UI: lo resuelve el repositorio, y de
     * paso guarda su nombre y foto desnormalizados, como hace createReview.
     */
    @Test
    fun `al crear se desnormaliza el perfil del organizador`() = runTest {
        // Arrange
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { userDataSource.getUser("uid-ana") } returns
            UserDto(fullName = "Ana Organiza", avatarUrl = "https://foto")
        val capturado = slot<NewEvent>()
        coEvery { eventDataSource.createEvent(capture(capturado)) } answers { dto() }

        // Act
        val resultado = repository.createEvent(draft())

        // Assert
        assertThat(resultado.isSuccess).isTrue()
        assertThat(capturado.captured.organizerId).isEqualTo("uid-ana")
        assertThat(capturado.captured.organizerName).isEqualTo("Ana Organiza")
        assertThat(capturado.captured.organizerAvatarUrl).isEqualTo("https://foto")
    }

    /** Si el perfil no se puede leer, se cae al nombre de la sesión. */
    @Test
    fun `sin perfil se usa el nombre de la sesion`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { userDataSource.getUser("uid-ana") } returns null
        val capturado = slot<NewEvent>()
        coEvery { eventDataSource.createEvent(capture(capturado)) } answers { dto() }

        repository.createEvent(draft())

        assertThat(capturado.captured.organizerName).isEqualTo("Ana")
    }

    @Test
    fun `el titulo se guarda sin espacios sobrantes`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { userDataSource.getUser(any()) } returns null
        val capturado = slot<NewEvent>()
        coEvery { eventDataSource.createEvent(capture(capturado)) } answers { dto() }

        repository.createEvent(draft(title = "  Pique en el autódromo  "))

        assertThat(capturado.captured.title).isEqualTo("Pique en el autódromo")
    }

    @Test
    fun `editar un evento ajeno falla sin tocar la fuente de datos`() = runTest {
        // Arrange
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { eventDataSource.getEventById("evt-1") } returns dto(userId = "uid-otro")

        // Act
        val resultado = repository.updateEvent("evt-1", draft())

        // Assert
        assertThat(resultado.isFailure).isTrue()
        assertThat(resultado.exceptionOrNull()?.message)
            .isEqualTo("Solo el organizador puede editar este evento")
        coVerify(exactly = 0) { eventDataSource.updateEvent(any(), any()) }
    }

    @Test
    fun `cancelar un evento ajeno falla sin tocar la fuente de datos`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { eventDataSource.getEventById("evt-1") } returns dto(userId = "uid-otro")

        val resultado = repository.cancelEvent("evt-1")

        assertThat(resultado.isFailure).isTrue()
        coVerify(exactly = 0) { eventDataSource.cancelEvent(any()) }
    }

    @Test
    fun `el organizador si puede cancelar su evento`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { eventDataSource.getEventById("evt-1") } returns dto(userId = "uid-ana")
        coEvery { eventDataSource.cancelEvent("evt-1") } returns Unit

        val resultado = repository.cancelEvent("evt-1")

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 1) { eventDataSource.cancelEvent("evt-1") }
    }

    // ---------- Errores traducidos ----------

    @Test
    fun `un IOException se traduce a sin conexion`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { userDataSource.getUser(any()) } returns null
        coEvery { eventDataSource.createEvent(any()) } throws IOException("socket")

        val resultado = repository.createEvent(draft())

        assertThat(resultado.exceptionOrNull()?.message).isEqualTo(NO_CONNECTION_MESSAGE)
    }

    /**
     * La traducción de los códigos de Firestore (PERMISSION_DENIED,
     * UNAVAILABLE, NOT_FOUND) no se puede probar aquí: construir
     * FirebaseFirestoreException.Code usa SparseArray de Android y revienta
     * en la JVM, como ya documenta ReviewRepositoryImplMockTest. Esa rama la
     * cubren las pruebas de integración contra el emulador; aquí se prueba
     * que cualquier otro fallo acaba en el mensaje genérico y no en el texto
     * crudo de la librería.
     */
    @Test
    fun `un error inesperado no filtra el mensaje de la libreria`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        coEvery { userDataSource.getUser(any()) } returns null
        coEvery { eventDataSource.createEvent(any()) } throws RuntimeException("Firestore internal blah")

        val resultado = repository.createEvent(draft())

        assertThat(resultado.exceptionOrNull()?.message).isEqualTo("No se pudo guardar el evento")
    }

    @Test
    fun `un evento inexistente da el mensaje de no existe`() = runTest {
        coEvery { eventDataSource.getEventById("evt-1") } throws NoSuchElementException("nope")

        val resultado = repository.getEventById("evt-1")

        assertThat(resultado.exceptionOrNull()?.message).isEqualTo("Este evento ya no existe")
    }

    /** Un documento mal formado no es un error: simplemente no existe para el dominio. */
    @Test
    fun `un documento mal formado se reporta como no existe`() = runTest {
        coEvery { eventDataSource.getEventById("evt-1") } returns dto(location = null)

        val resultado = repository.getEventById("evt-1")

        assertThat(resultado.isFailure).isTrue()
        assertThat(resultado.exceptionOrNull()?.message).isEqualTo("Este evento ya no existe")
    }

    // ---------- Flujo de eventos ----------

    @Test
    fun `los documentos mal formados se descartan sin tumbar la lista`() = runTest {
        every { eventDataSource.observeEventsBetween(any(), any()) } returns
            flowOf(listOf(dto(id = "ok"), dto(id = "roto", location = null)))

        val resultado = repository.observeEventsBetween(0L, inicio).first()

        assertThat(resultado.isSuccess).isTrue()
        assertThat(resultado.getOrNull()!!.map { it.id }).containsExactly("ok")
    }

    @Test
    fun `un error del listener sale como Result failure`() = runTest {
        every { eventDataSource.observeEventsBetween(any(), any()) } returns
            flow { throw IOException("el listener se cayó") }

        val resultado = repository.observeEventsBetween(0L, inicio).first()

        assertThat(resultado.isFailure).isTrue()
        assertThat(resultado.exceptionOrNull()?.message).isEqualTo("No se pudieron cargar los eventos")
    }

    // ---------- Participación ----------

    @Test
    fun `participar usa el uid de la sesion`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        givenParticipationChange(ParticipationChange.UNCHANGED)

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 1) {
            participationDataSource.setParticipation("evt-1", "uid-ana", ParticipationStatus.GOING)
        }
    }

    @Test
    fun `sin sesion no se puede participar`() = runTest {
        every { authDataSource.getCurrentUser() } returns null

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isFailure).isTrue()
        coVerify(exactly = 0) { participationDataSource.setParticipation(any(), any(), any()) }
    }

    /** Sin sesión la participación es null y no un error: es un adorno de la tarjeta. */
    @Test
    fun `sin sesion la participacion observada es null`() = runTest {
        every { authDataSource.getCurrentUser() } returns null

        assertThat(repository.observeMyParticipation("evt-1").first()).isNull()
    }

    @Test
    fun `un fallo al observar la participacion se degrada a null`() = runTest {
        every { authDataSource.getCurrentUser() } returns ana
        every { participationDataSource.observeMyParticipation("evt-1", "uid-ana") } returns
            flow { throw IOException("el listener se cayó") }

        assertThat(repository.observeMyParticipation("evt-1").first()).isNull()
    }

    // ---------- Aviso al organizador ----------

    @Test
    fun `marcar me interesa por primera vez avisa al organizador`() = runTest {
        givenBetoMarks(ParticipationChange.CREATED)

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.INTERESTED)

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 1) {
            notificationRepository.notify(
                recipientUid = "uid-ana",
                type = NotificationType.EVENT_INTERESTED,
                actorId = "uid-beto",
                eventId = "evt-1"
            )
        }
    }

    @Test
    fun `marcar voy por primera vez avisa al organizador`() = runTest {
        givenBetoMarks(ParticipationChange.CREATED)

        repository.setParticipation("evt-1", ParticipationStatus.GOING)

        coVerify(exactly = 1) {
            notificationRepository.notify(
                recipientUid = "uid-ana",
                type = NotificationType.EVENT_GOING,
                actorId = "uid-beto",
                eventId = "evt-1"
            )
        }
    }

    @Test
    fun `pasar de me interesa a voy no vuelve a avisar`() = runTest {
        givenBetoMarks(ParticipationChange.UPDATED)

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 0) { notificationRepository.notify(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `quitar la participacion no avisa`() = runTest {
        givenBetoMarks(ParticipationChange.REMOVED)

        val resultado = repository.setParticipation("evt-1", null)

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 0) { notificationRepository.notify(any(), any(), any(), any(), any(), any()) }
    }

    /**
     * El caso que justifica que la fuente devuelva [ParticipationChange] y no
     * el estado anterior: volver a marcar lo mismo sale de la transacción sin
     * escribir nada, y con un estado anterior nulable sería indistinguible de
     * "antes no había nada", así que un doble toque avisaría de la nada.
     */
    @Test
    fun `volver a marcar lo mismo no avisa`() = runTest {
        givenBetoMarks(ParticipationChange.UNCHANGED)

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 0) { notificationRepository.notify(any(), any(), any(), any(), any(), any()) }
    }

    /** El destinatario no lo elige la pantalla: sale del evento. */
    @Test
    fun `el destinatario sale del evento y no de la pantalla`() = runTest {
        every { authDataSource.getCurrentUser() } returns beto
        givenParticipationChange(ParticipationChange.CREATED)
        coEvery { eventDataSource.getEventById("evt-1") } returns dto(userId = "uid-carla")

        repository.setParticipation("evt-1", ParticipationStatus.GOING)

        coVerify(exactly = 1) { eventDataSource.getEventById("evt-1") }
        coVerify(exactly = 1) {
            notificationRepository.notify(
                recipientUid = "uid-carla",
                type = NotificationType.EVENT_GOING,
                actorId = "uid-beto",
                eventId = "evt-1"
            )
        }
    }

    @Test
    fun `no se lee el evento si no hubo marca nueva`() = runTest {
        givenBetoMarks(ParticipationChange.UPDATED)

        repository.setParticipation("evt-1", ParticipationStatus.GOING)

        coVerify(exactly = 0) { eventDataSource.getEventById(any()) }
    }

    /**
     * Avisar es secundario: la participación ya quedó escrita, así que un
     * fallo al leer el organizador no puede devolver failure ni hacer que el
     * ViewModel revierta el botón contra la verdad del servidor.
     */
    @Test
    fun `si no se puede leer el organizador la participacion sigue siendo exitosa`() = runTest {
        every { authDataSource.getCurrentUser() } returns beto
        givenParticipationChange(ParticipationChange.CREATED)
        coEvery { eventDataSource.getEventById("evt-1") } throws IOException("sin red")

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isSuccess).isTrue()
        coVerify(exactly = 0) { notificationRepository.notify(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `si avisar al organizador falla la participacion sigue siendo exitosa`() = runTest {
        givenBetoMarks(ParticipationChange.CREATED)
        coEvery {
            notificationRepository.notify(any(), any(), any(), any(), any(), any())
        } throws IOException("sin red")

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isSuccess).isTrue()
    }

    @Test
    fun `una participacion fallida no avisa a nadie`() = runTest {
        every { authDataSource.getCurrentUser() } returns beto
        coEvery {
            participationDataSource.setParticipation(any(), any(), any())
        } throws IOException("sin red")

        val resultado = repository.setParticipation("evt-1", ParticipationStatus.GOING)

        assertThat(resultado.isFailure).isTrue()
        coVerify(exactly = 0) { notificationRepository.notify(any(), any(), any(), any(), any(), any()) }
    }

    // ---------- Fixtures ----------

    private fun givenParticipationChange(change: ParticipationChange) {
        coEvery { participationDataSource.setParticipation(any(), any(), any()) } returns change
    }

    /** Beto marca en un evento que organiza Ana ("uid-ana", el dto por defecto). */
    private fun givenBetoMarks(change: ParticipationChange) {
        every { authDataSource.getCurrentUser() } returns beto
        givenParticipationChange(change)
        coEvery { eventDataSource.getEventById("evt-1") } returns dto()
    }

    private fun draft(title: String = "Encuentro en el 93") = EventDraft(
        type = EventType.CAR_MEET,
        title = title,
        description = "Nos vemos temprano",
        location = bogota,
        venueName = "Parque de la 93",
        address = "Calle 93 #13-50",
        startAt = inicio,
        features = setOf(EventFeature.EXHIBICION)
    )

    private fun dto(
        id: String = "evt-1",
        userId: String = "uid-ana",
        location: ReviewLocationSnapshot? = ReviewLocationSnapshot(lat = 4.6280, lng = -74.0643)
    ) = EventFirestoreDto(
        id = id,
        type = "CAR_MEET",
        title = "Encuentro en el 93",
        userId = userId,
        user = ReviewUserSnapshot(fullName = "Ana Organiza"),
        location = location,
        startAt = inicio
    )

    // ---------- Mis eventos (pestaña del perfil) ----------

    @Test
    fun `mis eventos junta los que organizo y a los que voy o me interesan`() = runTest {
        every { authDataSource.getCurrentUser() } returns beto
        every { eventDataSource.observeEventsBetween(any(), any()) } returns flowOf(
            listOf(
                dto(id = "propio", userId = "uid-beto"),
                dto(id = "voy"),
                dto(id = "interesa"),
                dto(id = "nada")
            )
        )
        coEvery { participationDataSource.getParticipation("voy", "uid-beto") } returns ParticipationStatus.GOING
        coEvery { participationDataSource.getParticipation("interesa", "uid-beto") } returns ParticipationStatus.INTERESTED
        coEvery { participationDataSource.getParticipation("nada", "uid-beto") } returns null

        val result = repository.getMyEvents(inicio).getOrThrow()

        assertThat(result.associate { it.event.id to it.role }).containsExactly(
            "propio", MyEventRole.ORGANIZER,
            "voy", MyEventRole.GOING,
            "interesa", MyEventRole.INTERESTED
        )
        // El propio no consulta participación: se sabe por el organizador.
        coVerify(exactly = 0) { participationDataSource.getParticipation("propio", any()) }
    }

    @Test
    fun `mis eventos sin sesion falla con mensaje`() = runTest {
        every { authDataSource.getCurrentUser() } returns null

        val result = repository.getMyEvents(inicio)

        assertThat(result.exceptionOrNull()?.message).isEqualTo("Debes iniciar sesión para ver tus eventos")
    }
}
