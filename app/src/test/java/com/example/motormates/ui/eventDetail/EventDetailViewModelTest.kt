package com.example.motormates.ui.eventDetail

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.Event
import com.example.motormates.data.model.EventOrganizer
import com.example.motormates.data.model.EventStatus
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ParticipationStatus
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * El detalle del evento con el repositorio mockeado. Lo que se prueba es la
 * participación optimista, la propiedad del evento y cancelar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var eventRepository: EventRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var timeSource: TimeSource

    private val now = 1_700_000_000_000L
    private val futuro = now + 24 * 60 * 60 * 1000L
    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643, placeName = "Chapinero")
    private val ana = AuthUser(id = "uid-ana", email = "ana@mail.com", displayName = "Ana")

    @Before
    fun setUp() {
        eventRepository = mockk()
        authRepository = mockk()
        timeSource = mockk()
        every { timeSource.nowMillis() } returns now
        every { authRepository.getCurrentUser() } returns ana
        every { eventRepository.observeMyParticipation(any()) } returns flowOf(null)
        coEvery { eventRepository.setParticipation(any(), any()) } returns Result.success(Unit)
    }

    private fun newViewModel() = EventDetailViewModel(eventRepository, authRepository, timeSource)

    private fun givenEvent(event: Event = event()) {
        coEvery { eventRepository.getEventById(event.id) } returns Result.success(event)
    }

    // ---------- Carga ----------

    @Test
    fun `solo el organizador ve editar y cancelar`() = runTest {
        givenEvent()
        val vm = newViewModel()

        vm.start("evt-1")
        advanceUntilIdle()

        assertThat(vm.uiState.value.event?.isMine).isTrue()
    }

    @Test
    fun `para otro usuario el evento no es propio`() = runTest {
        every { authRepository.getCurrentUser() } returns ana.copy(id = "uid-otro")
        givenEvent()
        val vm = newViewModel()

        vm.start("evt-1")
        advanceUntilIdle()

        assertThat(vm.uiState.value.event?.isMine).isFalse()
    }

    @Test
    fun `si no se pudo cargar queda el mensaje y no hay evento`() = runTest {
        coEvery { eventRepository.getEventById("evt-1") } returns
            Result.failure(Exception("Este evento ya no existe"))
        val vm = newViewModel()

        vm.start("evt-1")
        advanceUntilIdle()

        assertThat(vm.uiState.value.event).isNull()
        assertThat(vm.uiState.value.errorMessage).isEqualTo("Este evento ya no existe")
    }

    // ---------- Participación ----------

    @Test
    fun `marcar me interesa se pinta de inmediato`() = runTest {
        givenEvent()
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.toggleParticipation(ParticipationStatus.INTERESTED)

        // Sin esperar al servidor: lo pendiente ya manda.
        assertThat(vm.uiState.value.shownParticipation)
            .isEqualTo(ParticipationStatus.INTERESTED)
    }

    @Test
    fun `cambiar de me interesa a voy manda un solo cambio`() = runTest {
        givenEvent()
        every { eventRepository.observeMyParticipation("evt-1") } returns
            flowOf(ParticipationStatus.INTERESTED)
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.toggleParticipation(ParticipationStatus.GOING)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            eventRepository.setParticipation("evt-1", ParticipationStatus.GOING)
        }
    }

    /** Volver a tocar la respuesta que ya estaba la quita. */
    @Test
    fun `volver a marcar lo mismo quita la participacion`() = runTest {
        givenEvent()
        every { eventRepository.observeMyParticipation("evt-1") } returns
            flowOf(ParticipationStatus.GOING)
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.toggleParticipation(ParticipationStatus.GOING)
        advanceUntilIdle()

        coVerify(exactly = 1) { eventRepository.setParticipation("evt-1", null) }
    }

    @Test
    fun `si falla la escritura se revierte y se explica`() = runTest {
        givenEvent()
        coEvery { eventRepository.setParticipation(any(), any()) } returns
            Result.failure(Exception("No se pudo guardar tu respuesta"))
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.toggleParticipation(ParticipationStatus.GOING)
        advanceUntilIdle()

        assertThat(vm.uiState.value.shownParticipation).isNull()
        assertThat(vm.uiState.value.errorMessage).isEqualTo("No se pudo guardar tu respuesta")
    }

    /**
     * Cuando el servidor confirma, lo pendiente se descarta: a partir de ahí
     * manda el documento real, no lo que el usuario tocó.
     */
    @Test
    fun `cuando el servidor confirma manda lo confirmado`() = runTest {
        givenEvent()
        val participacion = MutableSharedFlow<ParticipationStatus?>(replay = 1)
        participacion.tryEmit(null)
        every { eventRepository.observeMyParticipation("evt-1") } returns participacion
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()
        vm.toggleParticipation(ParticipationStatus.GOING)
        advanceUntilIdle()

        participacion.tryEmit(ParticipationStatus.GOING)
        advanceUntilIdle()

        assertThat(vm.uiState.value.hasPendingParticipation).isFalse()
        assertThat(vm.uiState.value.myParticipation).isEqualTo(ParticipationStatus.GOING)
    }

    @Test
    fun `un evento cancelado no admite nuevas respuestas`() = runTest {
        givenEvent(event(status = EventStatus.CANCELLED))
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.toggleParticipation(ParticipationStatus.GOING)
        advanceUntilIdle()

        assertThat(vm.uiState.value.canParticipate).isFalse()
        coVerify(exactly = 0) { eventRepository.setParticipation(any(), any()) }
    }

    @Test
    fun `un evento que ya paso no admite nuevas respuestas`() = runTest {
        givenEvent(event(startAt = now - 10_000L))
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.toggleParticipation(ParticipationStatus.GOING)
        advanceUntilIdle()

        assertThat(vm.uiState.value.canParticipate).isFalse()
        coVerify(exactly = 0) { eventRepository.setParticipation(any(), any()) }
    }

    // ---------- Cancelar ----------

    @Test
    fun `cancelar pide confirmacion antes de escribir`() = runTest {
        givenEvent()
        coEvery { eventRepository.cancelEvent(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.askCancelEvent()

        assertThat(vm.uiState.value.showCancelDialog).isTrue()
        coVerify(exactly = 0) { eventRepository.cancelEvent(any()) }
    }

    @Test
    fun `al confirmar se cancela y la pantalla se cierra`() = runTest {
        givenEvent()
        coEvery { eventRepository.cancelEvent("evt-1") } returns Result.success(Unit)
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()
        vm.askCancelEvent()

        vm.confirmCancelEvent()
        advanceUntilIdle()

        assertThat(vm.uiState.value.isCancelled).isTrue()
        assertThat(vm.uiState.value.showCancelDialog).isFalse()
    }

    @Test
    fun `si cancelar falla no se cierra y queda el mensaje`() = runTest {
        givenEvent()
        coEvery { eventRepository.cancelEvent("evt-1") } returns
            Result.failure(Exception("No tienes permiso para hacer esto"))
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()
        vm.askCancelEvent()

        vm.confirmCancelEvent()
        advanceUntilIdle()

        assertThat(vm.uiState.value.isCancelled).isFalse()
        assertThat(vm.uiState.value.errorMessage).isEqualTo("No tienes permiso para hacer esto")
    }

    @Test
    fun `descartar el dialogo no cancela nada`() = runTest {
        givenEvent()
        coEvery { eventRepository.cancelEvent(any()) } returns Result.success(Unit)
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()
        vm.askCancelEvent()

        vm.dismissCancelDialog()
        advanceUntilIdle()

        assertThat(vm.uiState.value.showCancelDialog).isFalse()
        coVerify(exactly = 0) { eventRepository.cancelEvent(any()) }
    }

    private fun event(
        startAt: Long = futuro,
        status: EventStatus = EventStatus.SCHEDULED
    ) = Event(
        id = "evt-1",
        type = EventType.CAR_MEET,
        title = "Encuentro en el 93",
        description = "Nos vemos temprano",
        organizerId = "uid-ana",
        organizer = EventOrganizer(id = "uid-ana", fullName = "Ana Organiza"),
        location = bogota,
        venueName = "Parque de la 93",
        address = null,
        startAt = startAt,
        status = status,
        interestedCount = 3,
        goingCount = 7
    )
}
