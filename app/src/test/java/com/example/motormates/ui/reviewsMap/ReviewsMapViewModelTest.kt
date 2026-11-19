package com.example.motormates.ui.reviewsMap

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Event
import com.example.motormates.data.model.EventOrganizer
import com.example.motormates.data.model.EventStatus
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.model.ReviewVehicleRef
import com.example.motormates.data.model.Station
import com.example.motormates.data.model.StationKind
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.EventRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.StationRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ReviewsMapViewModel con el repositorio mockeado. Lo que se prueba es la
 * lógica propia del mapa: la ventana de 24 horas, descartar las reseñas sin
 * ubicación y el marcador seleccionado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsMapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var stationRepository: StationRepository
    private lateinit var deviceLocationDataSource: DeviceLocationDataSource
    private lateinit var timeSource: TimeSource
    private lateinit var viewModel: ReviewsMapViewModel

    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643)
    private val medellin = ReviewLocation(lat = 6.2442, lng = -75.5812)

    /** La "hora actual" del ViewModel: las pruebas la adelantan a mano. */
    private var now = System.currentTimeMillis()

    /** Los tics del reloj solo llegan cuando la prueba los emite. */
    private val ticks = MutableSharedFlow<Unit>()

    @Before
    fun setUp() {
        reviewRepository = mockk()
        vehicleRepository = mockk()
        eventRepository = mockk()
        authRepository = mockk()
        stationRepository = mockk()
        deviceLocationDataSource = mockk()
        timeSource = mockk()
        every { timeSource.nowMillis() } answers { now }
        every { timeSource.ticks(any()) } returns ticks
        // Por defecto el catálogo está vacío: las pruebas de fotos lo cambian.
        coEvery { vehicleRepository.getVehicles() } returns Result.success(emptyList())
        // La capa de eventos arranca apagada, así que estas respuestas por
        // defecto solo las usan las pruebas que la prenden.
        every { authRepository.getCurrentUser() } returns null
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.success(emptyList()))
        // Con llave configurada por defecto; la prueba de la compuerta la quita.
        every { stationRepository.isAvailable() } returns true
        coEvery { stationRepository.stationsNear(any(), any(), any(), any()) } returns
            Result.success(emptyList())
    }

    private fun newViewModel() = ReviewsMapViewModel(
        reviewRepository,
        vehicleRepository,
        eventRepository,
        authRepository,
        stationRepository,
        deviceLocationDataSource,
        timeSource
    )

    private fun viewModelWith(vararg reviews: Review): ReviewsMapViewModel {
        every { reviewRepository.observeRecentReviews(any()) } returns
            flowOf(Result.success(reviews.toList()))
        return newViewModel().also { viewModel = it }
    }

    @Test
    fun `solo las resenias con ubicacion generan marcador`() = runTest {
        val vm = viewModelWith(
            review(id = "con-ubicacion", location = bogota),
            review(id = "sin-ubicacion", location = null),
            review(id = "otra-con-ubicacion", location = medellin)
        )

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers.map { it.id })
            .containsExactly("con-ubicacion", "otra-con-ubicacion")
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    /**
     * El recorte en memoria: el listener sigue vivo mientras la pantalla esté
     * abierta, así que una reseña que ya cumplió 24 horas no debe quedarse
     * pegada en el mapa aunque la consulta la haya traído.
     */
    @Test
    fun `una resenia de mas de 24 horas no genera marcador`() = runTest {
        val vm = viewModelWith(
            review(id = "reciente", location = bogota, hoursAgo = 2),
            review(id = "vieja", location = medellin, hoursAgo = 30)
        )

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers.map { it.id }).containsExactly("reciente")
    }

    @Test
    fun `una resenia sin fecha legible no genera marcador`() = runTest {
        val vm = viewModelWith(review(id = "sin-fecha", location = bogota, createdAt = null))

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers).isEmpty()
    }

    /** Protege de confundir horas con millis al calcular la ventana. */
    @Test
    fun `la consulta pide la ventana de las ultimas 24 horas`() = runTest {
        val since = slot<Long>()
        every { reviewRepository.observeRecentReviews(capture(since)) } returns
            flowOf(Result.success(emptyList()))
        val vm = newViewModel()

        vm.start()
        advanceUntilIdle()

        val esperado = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        // Margen de 5 s: entre el cálculo del ViewModel y el del test pasa tiempo.
        assertThat(since.captured).isIn(esperado - 5_000..esperado + 5_000)
    }

    @Test
    fun `un error del repositorio deja el mensaje y ningun marcador`() = runTest {
        every { reviewRepository.observeRecentReviews(any()) } returns
            flowOf(Result.failure(Exception("No se pudieron cargar las reseñas")))
        val vm = newViewModel()

        vm.start()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.errorMessage).isEqualTo("No se pudieron cargar las reseñas")
        assertThat(state.markers).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.isEmpty).isFalse() // hay error: no es "vacío"
    }

    @Test
    fun `sin resenias el estado queda vacio y sin error`() = runTest {
        val vm = viewModelWith()

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.isEmpty).isTrue()
        assertThat(vm.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `tocar un marcador lo selecciona y clearSelection lo cierra`() = runTest {
        val vm = viewModelWith(
            review(id = "r1", location = bogota),
            review(id = "r2", location = medellin)
        )
        vm.start()
        advanceUntilIdle()

        vm.onMarkerClick("r2")
        assertThat(vm.uiState.value.selectedMarker?.id).isEqualTo("r2")

        vm.clearSelection()
        assertThat(vm.uiState.value.selectedMarker).isNull()
    }

    /**
     * Se guarda el id y no el objeto: si la reseña seleccionada desaparece de
     * la lista, la tarjeta se cierra sola en vez de mostrar datos viejos.
     */
    @Test
    fun `un marcador que no esta en la lista no se selecciona`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        vm.start()
        advanceUntilIdle()

        vm.onMarkerClick("borrada")

        assertThat(vm.uiState.value.selectedMarker).isNull()
    }

    @Test
    fun `start dos veces no duplica marcadores`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))

        vm.start()
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers).hasSize(1)
    }

    @Test
    fun `mientras no llega la primera emision el estado esta cargando`() = runTest {
        // Un flow que nunca emite: simula la espera del primer snapshot.
        every { reviewRepository.observeRecentReviews(any()) } returns flow { }
        val vm = newViewModel()

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isTrue()
    }

    // ---------- Foto del carro ----------

    @Test
    fun `el marcador lleva la foto del carro segun su vehicleId`() = runTest {
        coEvery { vehicleRepository.getVehicles() } returns
            Result.success(listOf(vehicle(id = "1", imageUrl = "https://porsche.jpg")))
        val vm = viewModelWith(review(id = "r1", location = bogota))

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers.single().vehicleImageUrl).isEqualTo("https://porsche.jpg")
    }

    @Test
    fun `si el catalogo falla los marcadores salen sin foto y sin error`() = runTest {
        coEvery { vehicleRepository.getVehicles() } returns Result.failure(Exception("sin red"))
        val vm = viewModelWith(review(id = "r1", location = bogota))

        vm.start()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.markers.single().vehicleImageUrl).isNull()
        assertThat(state.errorMessage).isNull()
    }

    // ---------- Reloj ----------

    @Test
    fun `con el tic del reloj sale del mapa la resenia que cumplio 24 horas`() = runTest {
        // Publicada hace 23 h 59 min: todavía entra en la ventana.
        val vm = viewModelWith(review(id = "casi-vieja", location = bogota, minutesAgo = 23 * 60 + 59))
        vm.start()
        advanceUntilIdle()
        assertThat(vm.uiState.value.markers).hasSize(1)

        // Pasan 2 minutos con el mapa abierto, sin que Firestore reemita.
        now += 2 * 60_000
        ticks.emit(Unit)
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers).isEmpty()
    }

    // ---------- Clusters ----------

    @Test
    fun `un grupo en el mismo punto abre la lista`() = runTest {
        val vm = viewModelWith(
            review(id = "r1", location = bogota),
            review(id = "r2", location = bogota)
        )
        vm.start()
        advanceUntilIdle()

        val openedList = vm.onClusterClick(listOf("r1", "r2"))

        assertThat(openedList).isTrue()
        assertThat(vm.uiState.value.clusterSelection.map { it.id }).containsExactly("r1", "r2")
    }

    @Test
    fun `un grupo en puntos distintos no abre la lista para que la pantalla acerque`() = runTest {
        val vm = viewModelWith(
            review(id = "r1", location = bogota),
            review(id = "r2", location = medellin)
        )
        vm.start()
        advanceUntilIdle()

        val openedList = vm.onClusterClick(listOf("r1", "r2"))

        assertThat(openedList).isFalse()
        assertThat(vm.uiState.value.clusterSelection).isEmpty()
    }

    @Test
    fun `un grupo de resenias a menos de 30 metros abre la lista`() = runTest {
        // ~20 m al norte de la primera: misma cuadra, puntos distintos.
        val vm = viewModelWith(
            review(id = "r1", location = bogota),
            review(id = "r2", location = bogota.copy(lat = bogota.lat + 20.0 / 111_320.0))
        )
        vm.start()
        advanceUntilIdle()

        val openedList = vm.onClusterClick(listOf("r1", "r2"))

        assertThat(openedList).isTrue()
        assertThat(vm.uiState.value.clusterSelection.map { it.id }).containsExactly("r1", "r2")
    }

    @Test
    fun `un grupo de resenias a mas de 30 metros deja que la pantalla acerque`() = runTest {
        val vm = viewModelWith(
            review(id = "r1", location = bogota),
            review(id = "r2", location = bogota.copy(lat = bogota.lat + 50.0 / 111_320.0))
        )
        vm.start()
        advanceUntilIdle()

        assertThat(vm.onClusterClick(listOf("r1", "r2"))).isFalse()
    }

    // ---------- Cámara ----------

    @Test
    fun `las resenias que llegan despues del primer encuadre cuentan como nuevas`() = runTest {
        val emissions = MutableSharedFlow<Result<List<Review>>>()
        every { reviewRepository.observeRecentReviews(any()) } returns emissions
        val vm = newViewModel()
        vm.start()
        advanceUntilIdle()
        emissions.emit(Result.success(listOf(review(id = "r1", location = bogota))))
        advanceUntilIdle()
        vm.onCameraFitted()

        emissions.emit(
            Result.success(listOf(review(id = "r1", location = bogota), review(id = "r2", location = medellin)))
        )
        advanceUntilIdle()

        assertThat(vm.uiState.value.newMarkersCount).isEqualTo(1)
    }

    // ---------- Mi ubicación ----------

    @Test
    fun `sin permiso centrar en mi ubicacion no consulta el gps`() = runTest {
        val vm = viewModelWith()
        vm.onLocationAccessChanged(LocationAccess.DENIED_CAN_ASK)

        vm.centerOnMyLocation()
        advanceUntilIdle()

        coVerify(exactly = 0) { deviceLocationDataSource.lastKnownLocation() }
        assertThat(vm.uiState.value.myLocationTarget).isNull()
    }

    @Test
    fun `con permiso centrar en mi ubicacion mueve la camara a donde estoy`() = runTest {
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns medellin
        val vm = viewModelWith()
        vm.onLocationAccessChanged(LocationAccess.GRANTED)

        vm.centerOnMyLocation()
        advanceUntilIdle()

        val target = vm.uiState.value.myLocationTarget
        assertThat(target?.lat).isEqualTo(medellin.lat)
        assertThat(target?.lng).isEqualTo(medellin.lng)
        assertThat(vm.uiState.value.isLocating).isFalse()
    }

    @Test
    fun `si no se obtiene la ubicacion se avisa al usuario`() = runTest {
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns null
        val vm = viewModelWith()
        vm.onLocationAccessChanged(LocationAccess.GRANTED)

        vm.centerOnMyLocation()
        advanceUntilIdle()

        assertThat(vm.uiState.value.locationMessage).isNotNull()
        assertThat(vm.uiState.value.myLocationTarget).isNull()
    }

    // ---------- Capas ----------

    @Test
    fun `al arrancar solo esta prendida la capa de resenias`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.layers).containsExactly(MapLayer.REVIEWS)
        assertThat(vm.uiState.value.eventMarkers).isEmpty()
    }

    @Test
    fun `prender la capa de eventos no toca los marcadores de resenias`() = runTest {
        // Arrange
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.success(listOf(event(id = "e1"))))
        vm.start()
        advanceUntilIdle()

        // Act
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        // Assert
        assertThat(vm.uiState.value.layers).containsExactly(MapLayer.REVIEWS, MapLayer.EVENTS)
        assertThat(vm.uiState.value.markers.map { it.id }).containsExactly("r1")
        assertThat(vm.uiState.value.eventMarkers.map { it.id }).containsExactly("e1")
    }

    @Test
    fun `apagar la capa de resenias no borra los eventos`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.success(listOf(event(id = "e1"))))
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        vm.toggleLayer(MapLayer.REVIEWS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers).isEmpty()
        assertThat(vm.uiState.value.eventMarkers.map { it.id }).containsExactly("e1")
    }

    @Test
    fun `apagar la capa de eventos borra sus marcadores y cierra su tarjeta`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.success(listOf(event(id = "e1"))))
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()
        vm.onEventMarkerClick("e1")

        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.eventMarkers).isEmpty()
        assertThat(vm.uiState.value.selection).isNull()
    }

    /** Apagar una capa no debe cerrar la tarjeta que está abierta de otra. */
    @Test
    fun `apagar los eventos no cierra la tarjeta de una resenia`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()
        vm.onMarkerClick("r1")

        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.selectedMarker?.id).isEqualTo("r1")
    }

    @Test
    fun `seleccionar un evento cierra la tarjeta de la resenia`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.success(listOf(event(id = "e1"))))
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()
        vm.onMarkerClick("r1")

        vm.onEventMarkerClick("e1")

        assertThat(vm.uiState.value.selectedMarker).isNull()
        assertThat(vm.uiState.value.selectedEvent?.id).isEqualTo("e1")
    }

    /**
     * Mismo contrato que las reseñas: la tarjeta se resuelve contra la lista
     * viva, así que un evento que desaparece cierra su propia tarjeta.
     */
    @Test
    fun `un evento que desaparece de la lista cierra su tarjeta sola`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        val eventos = MutableSharedFlow<Result<List<Event>>>(replay = 1)
        eventos.tryEmit(Result.success(listOf(event(id = "e1"))))
        every { eventRepository.observeEventsBetween(any(), any()) } returns eventos
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()
        vm.onEventMarkerClick("e1")

        eventos.tryEmit(Result.success(emptyList()))
        advanceUntilIdle()

        assertThat(vm.uiState.value.selectedEvent).isNull()
    }

    /**
     * Lo que la consulta no puede filtrar: el fin está en otro campo, y
     * filtrar por dos campos exigiría un índice compuesto.
     */
    @Test
    fun `los eventos que ya terminaron se descartan en memoria`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(
                Result.success(
                    listOf(
                        event(id = "pasado", startAt = now - 7_200_000L, endAt = now - 3_600_000L),
                        event(id = "futuro", startAt = now + 3_600_000L)
                    )
                )
            )
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.eventMarkers.map { it.id }).containsExactly("futuro")
    }

    /** Un cancelado sigue visible: quien dijo que iba necesita enterarse. */
    @Test
    fun `un evento cancelado sigue en el mapa`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(
                Result.success(
                    listOf(event(id = "e1", status = EventStatus.CANCELLED))
                )
            )
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.eventMarkers.map { it.id }).containsExactly("e1")
        assertThat(vm.uiState.value.eventMarkers.single().isCancelled).isTrue()
    }

    @Test
    fun `la consulta de eventos pide una ventana alrededor de ahora`() = runTest {
        val desde = slot<Long>()
        val hasta = slot<Long>()
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(capture(desde), capture(hasta)) } returns
            flowOf(Result.success(emptyList()))

        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        assertThat(desde.captured).isLessThan(now)
        assertThat(hasta.captured).isGreaterThan(now)
    }

    @Test
    fun `un fallo al cargar eventos deja el mensaje y no borra las resenias`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.failure(Exception("No se pudieron cargar los eventos")))
        vm.start()
        advanceUntilIdle()

        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isEqualTo("No se pudieron cargar los eventos")
        assertThat(vm.uiState.value.markers.map { it.id }).containsExactly("r1")
    }

    /**
     * start() nunca es idempotente: reabrir es lo que mantiene fresca la
     * ventana de tiempo al volver a la pestaña.
     */
    @Test
    fun `start vuelve a abrir la consulta de las capas prendidas`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()

        verify(atLeast = 2) { eventRepository.observeEventsBetween(any(), any()) }
    }

    @Test
    fun `el mapa vacio solo se anuncia si ninguna capa tiene marcadores`() = runTest {
        val vm = viewModelWith(review(id = "r1", location = bogota))
        every { eventRepository.observeEventsBetween(any(), any()) } returns
            flowOf(Result.success(listOf(event(id = "e1"))))
        vm.start()
        vm.toggleLayer(MapLayer.EVENTS)
        advanceUntilIdle()

        // Con eventos pero sin reseñas el mapa NO está vacío.
        vm.toggleLayer(MapLayer.REVIEWS)
        advanceUntilIdle()

        assertThat(vm.uiState.value.markers).isEmpty()
        assertThat(vm.uiState.value.isEmpty).isFalse()
    }

    // ---------- Capa de gasolineras ----------

    /**
     * El camino feliz, y de paso la prueba de que prender el chip consulta
     * SOLO: cameraFlow guarda la última posición, así que la capa arranca con
     * la que ya había sin esperar a que el usuario mueva el mapa.
     */
    @Test
    fun `prender la capa de gasolineras consulta con el centro y el radio de la camara`() =
        runTest {
            coEvery { stationRepository.stationsNear(any(), any(), any(), any()) } returns
                Result.success(listOf(station(id = "p1", name = "Terpel 85")))
            val vm = viewModelWith()
            vm.start()
            vm.onCameraIdle(camera())
            advanceUntilIdle()

            vm.toggleLayer(MapLayer.FUEL_STATIONS)
            advanceUntilIdle()

            assertThat(vm.uiState.value.stationsFor(MapLayer.FUEL_STATIONS).map { it.nombre })
                .containsExactly("Terpel 85")
            coVerify(exactly = 1) {
                stationRepository.stationsNear(
                    bogota.lat,
                    bogota.lng,
                    VISIBLE_RADIUS,
                    StationKind.FUEL
                )
            }
        }

    /**
     * El freno. Mientras el usuario arrastra, la cámara se detiene varias
     * veces; cada parada dentro de la ventana del debounce cancela la anterior,
     * así que un arrastre entero cuesta una sola llamada pagada.
     */
    @Test
    fun `varias paradas de camara seguidas cuestan una sola consulta`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        advanceUntilIdle()

        // Lejos unas de otras, para que no las descarte la guarda de zona.
        vm.onCameraIdle(camera(lat = 4.60))
        advanceTimeBy(100)
        vm.onCameraIdle(camera(lat = 4.70))
        advanceTimeBy(100)
        vm.onCameraIdle(camera(lat = 4.80))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            stationRepository.stationsNear(4.80, any(), any(), StationKind.FUEL)
        }
    }

    @Test
    fun `mover la camara poco no vuelve a consultar`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        // ~110 m: muy por debajo del 30 % de los 2 km de radio visible.
        vm.onCameraIdle(camera(lat = bogota.lat + 0.001))
        advanceUntilIdle()

        coVerify(exactly = 1) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    @Test
    fun `mover la camara lejos si vuelve a consultar`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        vm.onCameraIdle(camera(lat = medellin.lat, lng = medellin.lng))
        advanceUntilIdle()

        coVerify(exactly = 2) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    /**
     * Protege el TTL de confundirse de unidad: con 5 minutos en millis, mover
     * el reloj 6 minutos tiene que volver a abrir la consulta.
     *
     * Los dos movimientos son de ~11 m, muy dentro de la misma zona, así que
     * lo único que cambia entre el que no consulta y el que sí es el reloj.
     * Y son posiciones DISTINTAS a propósito: cameraFlow es un StateFlow, que
     * no reemite un valor igual al que ya tiene, así que repetir la misma
     * cámara no probaría nada.
     */
    @Test
    fun `pasado el TTL la misma zona se vuelve a consultar`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        vm.onCameraIdle(camera(lat = bogota.lat + 0.0001))
        advanceUntilIdle()
        coVerify(exactly = 1) { stationRepository.stationsNear(any(), any(), any(), any()) }

        now += 6 * 60 * 1000L
        vm.onCameraIdle(camera(lat = bogota.lat + 0.0002))
        advanceUntilIdle()

        coVerify(exactly = 2) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    @Test
    fun `con zoom corto no se consulta y se avisa`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)

        vm.onCameraIdle(camera(zoom = 10f))
        advanceUntilIdle()

        assertThat(vm.uiState.value.isStationsZoomTooLow).isTrue()
        assertThat(vm.uiState.value.stationsFor(MapLayer.FUEL_STATIONS)).isEmpty()
        coVerify(exactly = 0) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    /** Y al volver a acercar el aviso se va y la consulta sale. */
    @Test
    fun `al volver a acercar se consulta y se quita el aviso`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera(zoom = 10f))
        advanceUntilIdle()

        vm.onCameraIdle(camera(zoom = 15f))
        advanceUntilIdle()

        assertThat(vm.uiState.value.isStationsZoomTooLow).isFalse()
        coVerify(exactly = 1) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    /**
     * Lo importante de esta capa: que un fallo suyo NO se lleve el mapa por
     * delante. errorMessage pinta un aviso a pantalla completa, así que el
     * fallo de las estaciones va por su propio canal.
     */
    @Test
    fun `un fallo de gasolineras no borra las resenias ni pinta el error del mapa`() = runTest {
        coEvery { stationRepository.stationsNear(any(), any(), any(), any()) } returns
            Result.failure(Exception("La API key no tiene permiso para consultar lugares"))
        val vm = viewModelWith(review(id = "r1", location = bogota))
        vm.start()
        advanceUntilIdle()

        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.stationsMessage)
            .isEqualTo("La API key no tiene permiso para consultar lugares")
        assertThat(state.errorMessage).isNull()
        assertThat(state.markers.map { it.id }).containsExactly("r1")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `apagar la capa borra las gasolineras y deja de consultar`() = runTest {
        coEvery { stationRepository.stationsNear(any(), any(), any(), any()) } returns
            Result.success(listOf(station(id = "p1")))
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()
        assertThat(vm.uiState.value.stationsFor(MapLayer.FUEL_STATIONS)).hasSize(1)

        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera(lat = medellin.lat, lng = medellin.lng))
        advanceUntilIdle()

        assertThat(vm.uiState.value.stationsFor(MapLayer.FUEL_STATIONS)).isEmpty()
        coVerify(exactly = 1) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    /** Apagar y volver a prender tiene que consultar, aunque la zona siga fresca. */
    @Test
    fun `volver a prender la capa consulta de nuevo la misma zona`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        advanceUntilIdle()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        advanceUntilIdle()

        coVerify(exactly = 2) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    /** Sin radio no se sabe cuánto abarca la pantalla: no hay qué preguntar. */
    @Test
    fun `sin radio visible no se consulta`() = runTest {
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)

        vm.onCameraIdle(camera(radiusMeters = 0.0))
        advanceUntilIdle()

        coVerify(exactly = 0) { stationRepository.stationsNear(any(), any(), any(), any()) }
    }

    /** Las dos capas salen de la misma llave, así que se ofrecen o no juntas. */
    @Test
    fun `las capas de estaciones se ofrecen solo si hay API key`() = runTest {
        every { stationRepository.isAvailable() } returns true
        assertThat(viewModelWith().uiState.value.availableLayers).containsExactly(
            MapLayer.REVIEWS,
            MapLayer.EVENTS,
            MapLayer.FUEL_STATIONS,
            MapLayer.CHARGING_STATIONS
        )

        every { stationRepository.isAvailable() } returns false
        assertThat(viewModelWith().uiState.value.availableLayers)
            .containsExactly(MapLayer.REVIEWS, MapLayer.EVENTS)
    }

    // ---------- Capa de puntos de carga ----------

    @Test
    fun `la capa de carga consulta con su propio tipo`() = runTest {
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.CHARGING)
        } returns Result.success(listOf(station(id = "c1", name = "Celsia Carga")))
        val vm = viewModelWith()
        vm.start()

        vm.toggleLayer(MapLayer.CHARGING_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        val cargadores = vm.uiState.value.stationsFor(MapLayer.CHARGING_STATIONS)
        assertThat(cargadores.map { it.nombre }).containsExactly("Celsia Carga")
        assertThat(cargadores.single().kind).isEqualTo(StationKind.CHARGING)
        coVerify(exactly = 0) {
            stationRepository.stationsNear(any(), any(), any(), StationKind.FUEL)
        }
    }

    /**
     * La razón de que la guarda de zona sea por capa: con una compartida, la
     * segunda capa se saltaría su consulta porque la primera ya marcó la zona
     * como vista, y se quedaría vacía para siempre.
     */
    @Test
    fun `con las dos capas prendidas cada una hace su consulta`() = runTest {
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.FUEL)
        } returns Result.success(listOf(station(id = "f1", name = "Terpel")))
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.CHARGING)
        } returns Result.success(listOf(station(id = "c1", name = "Celsia")))
        val vm = viewModelWith()
        vm.start()

        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.toggleLayer(MapLayer.CHARGING_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.stationsFor(MapLayer.FUEL_STATIONS).map { it.id }).containsExactly("f1")
        assertThat(state.stationsFor(MapLayer.CHARGING_STATIONS).map { it.id })
            .containsExactly("c1")
        coVerify(exactly = 1) {
            stationRepository.stationsNear(any(), any(), any(), StationKind.FUEL)
        }
        coVerify(exactly = 1) {
            stationRepository.stationsNear(any(), any(), any(), StationKind.CHARGING)
        }
    }

    @Test
    fun `apagar una capa de estaciones no toca la otra`() = runTest {
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.FUEL)
        } returns Result.success(listOf(station(id = "f1")))
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.CHARGING)
        } returns Result.success(listOf(station(id = "c1")))
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.toggleLayer(MapLayer.CHARGING_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.stationsFor(MapLayer.FUEL_STATIONS)).isEmpty()
        assertThat(state.stationsFor(MapLayer.CHARGING_STATIONS).map { it.id })
            .containsExactly("c1")
    }

    /** La tarjeta abierta se resuelve en la lista de SU capa, no en la otra. */
    @Test
    fun `la estacion seleccionada sale de la capa que la abrio`() = runTest {
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.FUEL)
        } returns Result.success(listOf(station(id = "mismo-id", name = "Terpel")))
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.CHARGING)
        } returns Result.success(listOf(station(id = "mismo-id", name = "Celsia")))
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.toggleLayer(MapLayer.CHARGING_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        vm.onStationMarkerClick(MapLayer.CHARGING_STATIONS, "mismo-id")

        assertThat(vm.uiState.value.selectedStation?.nombre).isEqualTo("Celsia")
    }

    /** Un fallo de una capa no borra los marcadores de la otra. */
    @Test
    fun `un fallo de carga no borra las gasolineras`() = runTest {
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.FUEL)
        } returns Result.success(listOf(station(id = "f1")))
        coEvery {
            stationRepository.stationsNear(any(), any(), any(), StationKind.CHARGING)
        } returns Result.failure(Exception("No se pudieron cargar los puntos de carga"))
        val vm = viewModelWith()
        vm.start()
        vm.toggleLayer(MapLayer.FUEL_STATIONS)
        vm.toggleLayer(MapLayer.CHARGING_STATIONS)
        vm.onCameraIdle(camera())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.stationsMessage).isEqualTo("No se pudieron cargar los puntos de carga")
        assertThat(state.stationsFor(MapLayer.FUEL_STATIONS).map { it.id }).containsExactly("f1")
        assertThat(state.errorMessage).isNull()
        assertThat(state.isLoading).isFalse()
    }

    /** El aviso de "acércate" aparece por el zoom, no por una capa en concreto. */
    @Test
    fun `sin ninguna capa de estaciones no hay aviso de zoom`() = runTest {
        val vm = viewModelWith()
        vm.start()

        vm.onCameraIdle(camera(zoom = 10f))
        advanceUntilIdle()

        assertThat(vm.uiState.value.isStationsZoomTooLow).isFalse()
    }

    private fun camera(
        lat: Double = bogota.lat,
        lng: Double = bogota.lng,
        zoom: Float = 15f,
        radiusMeters: Double = VISIBLE_RADIUS
    ) = MapCamera(lat = lat, lng = lng, zoom = zoom, radiusMeters = radiusMeters)

    private fun station(id: String, name: String? = "Terpel", address: String? = "Calle 85") =
        Station(id = id, name = name, address = address, location = bogota)

    private companion object {
        /** 2 km: lo que abarca la pantalla a zoom de barrio, más o menos. */
        const val VISIBLE_RADIUS = 2_000.0
    }

    private fun event(
        id: String,
        startAt: Long = now + 3_600_000L,
        endAt: Long? = null,
        status: EventStatus =
            EventStatus.SCHEDULED
    ) = Event(
        id = id,
        type = EventType.CAR_MEET,
        title = "Encuentro en el 93",
        description = null,
        organizerId = "uid-ana",
        organizer = EventOrganizer(
            id = "uid-ana",
            fullName = "Ana Organiza"
        ),
        location = bogota,
        venueName = "Parque de la 93",
        address = null,
        startAt = startAt,
        endAt = endAt,
        status = status
    )

    private fun vehicle(id: String, imageUrl: String?) = Vehicle(
        id = id, brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = imageUrl, potencia = null, aceleracion = null, velocidadMaxima = null, traccion = null
    )

    private fun review(
        id: String,
        location: ReviewLocation?,
        hoursAgo: Long = 1,
        minutesAgo: Long = hoursAgo * 60,
        createdAt: String? = Instant.ofEpochMilli(now).minusSeconds(minutesAgo * 60).toString()
    ) = Review(
        id = id,
        rating = 5,
        comment = "Brutal",
        userId = "uid-jose",
        vehicleId = "1",
        createdAt = createdAt,
        author = ReviewAuthor(id = "uid-jose", fullName = "Jose Perfil"),
        vehicle = ReviewVehicleRef(id = "1", brand = "Porsche", model = "911 GT3"),
        location = location
    )
}
