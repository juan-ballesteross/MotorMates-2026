package com.example.motormates.ui.newEvent

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Event
import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventOrganizer
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.repository.EventDraft
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * El formulario de crear y editar eventos, con el repositorio mockeado. Lo
 * que se prueba es su lógica propia: las validaciones, que cambiar de tipo
 * descarte lo del tipo anterior y que no se publique dos veces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewEventViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var eventRepository: EventRepository
    private lateinit var timeSource: TimeSource

    private val now = 1_700_000_000_000L
    private val futuro = now + 24 * 60 * 60 * 1000L
    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643, placeName = "Chapinero")

    @Before
    fun setUp() {
        eventRepository = mockk()
        timeSource = mockk()
        every { timeSource.nowMillis() } returns now
    }

    private fun newViewModel() = NewEventViewModel(eventRepository, timeSource)

    /** Un formulario listo para publicar, para que cada prueba rompa una sola cosa. */
    private fun NewEventViewModel.fillValidForm() {
        updateTitle("Encuentro en el 93")
        updateLocation(bogota)
        updateStartAt(futuro)
    }

    // ---------- Tipo y características ----------

    @Test
    fun `al arrancar el tipo es car meet y solo ofrece sus caracteristicas`() {
        val vm = newViewModel()

        assertThat(vm.uiState.value.type).isEqualTo(EventType.CAR_MEET)
        assertThat(vm.uiState.value.availableFeatures)
            .containsExactlyElementsIn(EventFeature.entries.filter { it.type == EventType.CAR_MEET })
    }

    @Test
    fun `cambiar el tipo descarta los atributos del tipo anterior`() {
        // Arrange
        val vm = newViewModel()
        vm.toggleFeature(EventFeature.FOOD_TRUCKS)
        vm.toggleFeature(EventFeature.MUSICA)

        // Act
        vm.updateType(EventType.PIQUE)

        // Assert
        assertThat(vm.uiState.value.selectedFeatures).isEmpty()
        assertThat(vm.uiState.value.availableFeatures)
            .containsExactlyElementsIn(EventFeature.entries.filter { it.type == EventType.PIQUE })
    }

    @Test
    fun `alternar una caracteristica la marca y la desmarca`() {
        val vm = newViewModel()

        vm.toggleFeature(EventFeature.EXHIBICION)
        assertThat(vm.uiState.value.selectedFeatures).containsExactly(EventFeature.EXHIBICION)

        vm.toggleFeature(EventFeature.EXHIBICION)
        assertThat(vm.uiState.value.selectedFeatures).isEmpty()
    }

    // ---------- Validaciones ----------

    @Test
    fun `sin titulo no se publica y queda el mensaje`() = runTest {
        val vm = newViewModel()
        vm.updateLocation(bogota)
        vm.updateStartAt(futuro)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("El evento necesita un título")
        coVerify(exactly = 0) { eventRepository.createEvent(any()) }
    }

    @Test
    fun `sin fecha de inicio no se publica y queda el mensaje`() = runTest {
        val vm = newViewModel()
        vm.updateTitle("Encuentro en el 93")
        vm.updateLocation(bogota)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("Elige la fecha y la hora de inicio")
        coVerify(exactly = 0) { eventRepository.createEvent(any()) }
    }

    @Test
    fun `sin ubicacion no se publica y queda el mensaje`() = runTest {
        val vm = newViewModel()
        vm.updateTitle("Encuentro en el 93")
        vm.updateStartAt(futuro)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage)
            .isEqualTo("Elige en el mapa dónde será el evento")
        coVerify(exactly = 0) { eventRepository.createEvent(any()) }
    }

    @Test
    fun `una hora de fin anterior al inicio no se publica`() = runTest {
        val vm = newViewModel()
        vm.fillValidForm()
        // Se pone el fin antes del inicio pasando por updateEndAt directo:
        // la UI lo impide, pero el ViewModel es la autoridad.
        vm.updateEndAt(futuro - 1)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage)
            .isEqualTo("La hora de fin debe ser después de la de inicio")
        coVerify(exactly = 0) { eventRepository.createEvent(any()) }
    }

    @Test
    fun `un evento en el pasado no se publica al crear`() = runTest {
        val vm = newViewModel()
        vm.updateTitle("Encuentro en el 93")
        vm.updateLocation(bogota)
        vm.updateStartAt(now - 1)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("La fecha de inicio ya pasó")
    }

    /**
     * Al editar sí se permite una fecha pasada: puede hacer falta corregir un
     * dato de un evento que ya ocurrió.
     */
    @Test
    fun `al editar si se admite una fecha que ya paso`() = runTest {
        coEvery { eventRepository.getEventById("evt-1") } returns Result.success(event())
        coEvery { eventRepository.updateEvent(any(), any()) } returns Result.success(event())
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()
        vm.updateStartAt(now - 10_000L)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isNull()
        assertThat(vm.uiState.value.isSaved).isTrue()
    }

    /** Mover el inicio más allá del fin descarta el fin en vez de dejar un rango imposible. */
    @Test
    fun `mover el inicio despues del fin borra el fin`() {
        val vm = newViewModel()
        vm.updateStartAt(futuro)
        vm.updateEndAt(futuro + 3_600_000L)

        vm.updateStartAt(futuro + 7_200_000L)

        assertThat(vm.uiState.value.endAt).isNull()
    }

    // ---------- Ubicación ----------

    @Test
    fun `elegir un punto llena la ubicacion y propone el nombre del sitio`() {
        val vm = newViewModel()

        vm.updateLocation(bogota)

        assertThat(vm.uiState.value.location).isEqualTo(bogota)
        assertThat(vm.uiState.value.venueName).isEqualTo("Chapinero")
    }

    /** Lo que el usuario escribió nunca se pisa con lo del geocodificador. */
    @Test
    fun `elegir un punto no pisa el nombre que el usuario ya escribio`() {
        val vm = newViewModel()
        vm.updateVenueName("Parqueadero del centro comercial")

        vm.updateLocation(bogota)

        assertThat(vm.uiState.value.venueName).isEqualTo("Parqueadero del centro comercial")
    }

    // ---------- Guardar ----------

    @Test
    fun `al publicar se mandan solo los atributos del tipo elegido`() = runTest {
        // Arrange
        val enviado = slot<EventDraft>()
        coEvery { eventRepository.createEvent(capture(enviado)) } returns Result.success(event())
        val vm = newViewModel()
        vm.fillValidForm()
        vm.toggleFeature(EventFeature.EXHIBICION)
        vm.updateType(EventType.PIQUE)
        vm.toggleFeature(EventFeature.CASCO_OBLIGATORIO)

        // Act
        vm.save()
        advanceUntilIdle()

        // Assert
        assertThat(enviado.captured.type).isEqualTo(EventType.PIQUE)
        assertThat(enviado.captured.features).containsExactly(EventFeature.CASCO_OBLIGATORIO)
    }

    @Test
    fun `los textos en blanco se mandan como null`() = runTest {
        val enviado = slot<EventDraft>()
        coEvery { eventRepository.createEvent(capture(enviado)) } returns Result.success(event())
        val vm = newViewModel()
        vm.fillValidForm()
        vm.updateDescription("   ")

        vm.save()
        advanceUntilIdle()

        assertThat(enviado.captured.description).isNull()
    }

    @Test
    fun `al publicar con exito el estado dice que ya se guardo`() = runTest {
        coEvery { eventRepository.createEvent(any()) } returns Result.success(event())
        val vm = newViewModel()
        vm.fillValidForm()

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.isSaved).isTrue()
        assertThat(vm.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `si el repositorio falla se muestra el mensaje y no se cierra`() = runTest {
        coEvery { eventRepository.createEvent(any()) } returns
            Result.failure(Exception("Sin conexión con el servidor"))
        val vm = newViewModel()
        vm.fillValidForm()

        vm.save()
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("Sin conexión con el servidor")
        assertThat(vm.uiState.value.isSaved).isFalse()
    }

    @Test
    fun `publicar dos veces seguidas solo guarda una vez`() = runTest {
        coEvery { eventRepository.createEvent(any()) } returns Result.success(event())
        val vm = newViewModel()
        vm.fillValidForm()

        vm.save()
        vm.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { eventRepository.createEvent(any()) }
    }

    // ---------- Edición ----------

    @Test
    fun `start en modo edicion precarga el formulario`() = runTest {
        coEvery { eventRepository.getEventById("evt-1") } returns Result.success(
            event(
                type = EventType.PIQUE,
                features = setOf(EventFeature.CRONOMETRAJE)
            )
        )
        val vm = newViewModel()

        vm.start("evt-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.title).isEqualTo("Encuentro en el 93")
        assertThat(state.type).isEqualTo(EventType.PIQUE)
        assertThat(state.selectedFeatures).containsExactly(EventFeature.CRONOMETRAJE)
        assertThat(state.location).isEqualTo(bogota)
    }

    @Test
    fun `start dos veces no vuelve a pedir el evento`() = runTest {
        coEvery { eventRepository.getEventById("evt-1") } returns Result.success(event())
        val vm = newViewModel()

        vm.start("evt-1")
        advanceUntilIdle()
        vm.start("evt-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { eventRepository.getEventById("evt-1") }
    }

    @Test
    fun `al editar se llama a updateEvent y no a createEvent`() = runTest {
        coEvery { eventRepository.getEventById("evt-1") } returns Result.success(event())
        coEvery { eventRepository.updateEvent("evt-1", any()) } returns Result.success(event())
        val vm = newViewModel()
        vm.start("evt-1")
        advanceUntilIdle()

        vm.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { eventRepository.updateEvent("evt-1", any()) }
        coVerify(exactly = 0) { eventRepository.createEvent(any()) }
    }

    @Test
    fun `si no se pudo cargar el evento queda el mensaje`() = runTest {
        coEvery { eventRepository.getEventById("evt-1") } returns
            Result.failure(Exception("Este evento ya no existe"))
        val vm = newViewModel()

        vm.start("evt-1")
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("Este evento ya no existe")
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    private fun event(
        type: EventType = EventType.CAR_MEET,
        features: Set<EventFeature> = emptySet()
    ) = Event(
        id = "evt-1",
        type = type,
        title = "Encuentro en el 93",
        description = "Nos vemos temprano",
        organizerId = "uid-ana",
        organizer = EventOrganizer(id = "uid-ana", fullName = "Ana Organiza"),
        location = bogota,
        venueName = "Parque de la 93",
        address = null,
        startAt = futuro,
        features = features
    )
}
