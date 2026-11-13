package com.example.motormates.ui.reviewsMap

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.model.ReviewVehicleRef
import com.example.motormates.data.repository.ReviewRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var viewModel: ReviewsMapViewModel

    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643)
    private val medellin = ReviewLocation(lat = 6.2442, lng = -75.5812)

    @Before
    fun setUp() {
        reviewRepository = mockk()
    }

    private fun viewModelWith(vararg reviews: Review): ReviewsMapViewModel {
        every { reviewRepository.observeRecentReviews(any()) } returns
            flowOf(Result.success(reviews.toList()))
        return ReviewsMapViewModel(reviewRepository).also { viewModel = it }
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
        val vm = ReviewsMapViewModel(reviewRepository)

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
        val vm = ReviewsMapViewModel(reviewRepository)

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
        val vm = ReviewsMapViewModel(reviewRepository)

        vm.start()
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isTrue()
    }

    private fun review(
        id: String,
        location: ReviewLocation?,
        hoursAgo: Long = 1,
        createdAt: String? = Instant.now().minusSeconds(hoursAgo * 3600).toString()
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
