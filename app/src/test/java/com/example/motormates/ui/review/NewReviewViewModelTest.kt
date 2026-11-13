package com.example.motormates.ui.review

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.source.PlaceNameDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * NewReviewViewModel con los repositorios mockeados. Cada prueba lanza la
 * acción y llama a advanceUntilIdle() antes de los asserts, para que las
 * corrutinas de viewModelScope hayan terminado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var deviceLocationDataSource: DeviceLocationDataSource
    private lateinit var placeNameDataSource: PlaceNameDataSource
    private lateinit var viewModel: NewReviewViewModel

    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643)

    private val porsche = Vehicle(
        id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = null, potencia = null, aceleracion = null, velocidadMaxima = null, traccion = null
    )

    @Before
    fun setUp() {
        vehicleRepository = mockk()
        reviewRepository = mockk()
        deviceLocationDataSource = mockk()
        placeNameDataSource = mockk()
        // Por defecto, sin ubicación: es el caso más común (el usuario no dio
        // el permiso o el dispositivo no tiene posición todavía).
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns null
        coEvery { placeNameDataSource.placeNameFor(any()) } returns null
        viewModel = NewReviewViewModel(
            vehicleRepository,
            reviewRepository,
            deviceLocationDataSource,
            placeNameDataSource
        )
    }

    // ---------- start ----------

    @Test
    fun `start al crear carga el resumen del vehiculo con el formulario vacio`() = runTest {
        // Arrange
        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche)

        // Act
        viewModel.start(vehicleId = "1", reviewId = null)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.car?.title).isEqualTo("Porsche 911 GT3")
        assertThat(state.car?.year).isEqualTo(2024)
        assertThat(state.isLoading).isFalse()
        assertThat(state.rating).isEqualTo(0)
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `start si el vehiculo no carga muestra el error del repositorio`() = runTest {
        coEvery { vehicleRepository.getVehicleById("1") } returns
            Result.failure(Exception("Este vehículo no existe o fue eliminado"))

        viewModel.start(vehicleId = "1", reviewId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.car).isNull()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Este vehículo no existe o fue eliminado")
    }

    @Test
    fun `start al editar precarga la calificacion y el comentario de la resenia`() = runTest {
        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche)
        coEvery { reviewRepository.getReviewsByVehicle("1") } returns
            Result.success(listOf(review(id = "r1", rating = 3, comment = "Regular")))

        viewModel.start(vehicleId = "1", reviewId = "r1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.reviewId).isEqualTo("r1")
        assertThat(state.rating).isEqualTo(3)
        assertThat(state.experience).isEqualTo("Regular")
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `start al editar una resenia que ya no existe muestra mensaje`() = runTest {
        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche)
        coEvery { reviewRepository.getReviewsByVehicle("1") } returns Result.success(emptyList())

        viewModel.start(vehicleId = "1", reviewId = "borrada")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.reviewId).isNull()
        assertThat(state.errorMessage).isEqualTo("No se encontró la reseña que querías editar")
    }

    @Test
    fun `start llamado dos veces solo carga el vehiculo una vez`() = runTest {
        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche)

        viewModel.start(vehicleId = "1", reviewId = null)
        advanceUntilIdle()
        viewModel.start(vehicleId = "1", reviewId = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { vehicleRepository.getVehicleById("1") }
    }

    // ---------- save ----------

    @Test
    fun `save sin calificacion muestra error y no llama al repositorio`() = runTest {
        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errorMessage)
            .isEqualTo("Selecciona una calificación de 1 a 5 estrellas")
        assertThat(viewModel.uiState.value.isSaved).isFalse()
        coVerify(exactly = 0) { reviewRepository.createReview(any(), any(), any(), any()) }
    }

    @Test
    fun `save al crear con comentario en blanco envia null y marca guardado`() = runTest {
        coEvery { reviewRepository.createReview("1", 4, null, null) } returns Result.success(review(id = "nueva", rating = 4))
        viewModel.updateRating(4)
        viewModel.updateExperience("   ")

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        coVerify(exactly = 1) { reviewRepository.createReview("1", 4, null, null) }
        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `save al editar actualiza la resenia con el texto recortado`() = runTest {
        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche)
        coEvery { reviewRepository.getReviewsByVehicle("1") } returns
            Result.success(listOf(review(id = "r1", rating = 3, comment = "Regular")))
        coEvery { reviewRepository.updateReview("r1", 5, "Mejoró mucho") } returns
            Result.success(review(id = "r1", rating = 5, comment = "Mejoró mucho"))
        viewModel.start(vehicleId = "1", reviewId = "r1")
        advanceUntilIdle()
        viewModel.updateRating(5)
        viewModel.updateExperience("  Mejoró mucho  ")

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        coVerify(exactly = 1) { reviewRepository.updateReview("r1", 5, "Mejoró mucho") }
        coVerify(exactly = 0) { reviewRepository.createReview(any(), any(), any(), any()) }
        assertThat(viewModel.uiState.value.isSaved).isTrue()
    }

    // ---------- ubicación ----------

    @Test
    fun `captureLocation deja la ubicacion con su nombre en el estado`() = runTest {
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns bogota
        coEvery { placeNameDataSource.placeNameFor(bogota) } returns "Chapinero, Bogotá"

        viewModel.captureLocation()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.capturedLocation?.lat).isEqualTo(bogota.lat)
        assertThat(state.capturedLocation?.placeName).isEqualTo("Chapinero, Bogotá")
        assertThat(state.isLocationLoading).isFalse()
        assertThat(state.isLocationUnavailable).isFalse()
    }

    /**
     * El nombre es un adorno: si el geocodificador no responde, la ubicación
     * sigue guardándose y la reseña sale en el mapa, solo sin texto.
     */
    @Test
    fun `captureLocation sin nombre guarda igual la ubicacion`() = runTest {
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns bogota
        coEvery { placeNameDataSource.placeNameFor(bogota) } returns null

        viewModel.captureLocation()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.capturedLocation).isNotNull()
        assertThat(state.capturedLocation?.placeName).isNull()
        assertThat(state.isLocationUnavailable).isFalse()
    }

    @Test
    fun `captureLocation sin ubicacion marca que no esta disponible`() = runTest {
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns null

        viewModel.captureLocation()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.capturedLocation).isNull()
        assertThat(state.isLocationUnavailable).isTrue()
        assertThat(state.isLocationLoading).isFalse()
        // No se pide el nombre de algo que no existe.
        coVerify(exactly = 0) { placeNameDataSource.placeNameFor(any()) }
    }

    @Test
    fun `removeLocation descarta la ubicacion capturada`() = runTest {
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns bogota
        coEvery { placeNameDataSource.placeNameFor(bogota) } returns "Chapinero, Bogotá"
        viewModel.captureLocation()
        advanceUntilIdle()

        viewModel.removeLocation()

        val state = viewModel.uiState.value
        assertThat(state.capturedLocation).isNull()
        // Quitarla a propósito no es lo mismo que no haberla conseguido.
        assertThat(state.isLocationUnavailable).isFalse()
    }

    @Test
    fun `save manda la ubicacion que ya se habia capturado`() = runTest {
        val enviada = slot<ReviewLocation>()
        coEvery { deviceLocationDataSource.lastKnownLocation() } returns bogota
        coEvery { placeNameDataSource.placeNameFor(bogota) } returns "Chapinero, Bogotá"
        coEvery { reviewRepository.createReview("1", 5, null, capture(enviada)) } returns
            Result.success(review(id = "nueva", rating = 5))
        viewModel.captureLocation()
        advanceUntilIdle()
        viewModel.updateRating(5)

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        assertThat(enviada.captured.placeName).isEqualTo("Chapinero, Bogotá")
        assertThat(viewModel.uiState.value.isSaved).isTrue()
    }

    /**
     * El acuerdo con el cliente: si no hay ubicación (permiso negado, sin GPS)
     * o el usuario la quitó, la reseña se publica igual y solo no aparece en
     * el mapa.
     */
    @Test
    fun `save sin ubicacion publica la resenia igual`() = runTest {
        coEvery { reviewRepository.createReview("1", 5, null, null) } returns
            Result.success(review(id = "nueva", rating = 5))
        viewModel.updateRating(5)

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        coVerify(exactly = 1) { reviewRepository.createReview("1", 5, null, null) }
        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /**
     * Publicar no espera al GPS: la ubicación ya venía resuelta en el estado.
     * Esto es lo que permitió quitar el bloqueo del botón mientras se pedía
     * el permiso (antes dos toques seguidos publicaban dos veces).
     */
    @Test
    fun `save no consulta la ubicacion al dispositivo`() = runTest {
        coEvery { reviewRepository.createReview(any(), any(), any(), any()) } returns
            Result.success(review(id = "nueva", rating = 5))
        viewModel.updateRating(5)

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        coVerify(exactly = 0) { deviceLocationDataSource.lastKnownLocation() }
    }

    @Test
    fun `save si el repositorio falla muestra su mensaje y no marca guardado`() = runTest {
        coEvery { reviewRepository.createReview(any(), any(), any(), any()) } returns
            Result.failure(Exception("No se pudo publicar la reseña"))
        viewModel.updateRating(5)

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isEqualTo("No se pudo publicar la reseña")
        assertThat(state.isSaving).isFalse()
        assertThat(state.isSaved).isFalse()
    }

    private fun review(id: String, rating: Int, comment: String? = null) = Review(
        id = id,
        rating = rating,
        comment = comment,
        userId = "uid-jose",
        vehicleId = "1",
        createdAt = null,
        author = null,
        vehicle = null
    )
}
