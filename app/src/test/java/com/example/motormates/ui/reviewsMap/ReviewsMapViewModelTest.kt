package com.example.motormates.ui.reviewsMap

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.model.ReviewVehicleRef
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
        deviceLocationDataSource = mockk()
        timeSource = mockk()
        every { timeSource.nowMillis() } answers { now }
        every { timeSource.ticks(any()) } returns ticks
        // Por defecto el catálogo está vacío: las pruebas de fotos lo cambian.
        coEvery { vehicleRepository.getVehicles() } returns Result.success(emptyList())
    }

    private fun newViewModel() =
        ReviewsMapViewModel(reviewRepository, vehicleRepository, deviceLocationDataSource, timeSource)

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
