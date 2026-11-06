package com.example.motormates.ui.review

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private lateinit var viewModel: NewReviewViewModel

    private val porsche = Vehicle(
        id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = null, potencia = null, aceleracion = null, velocidadMaxima = null, traccion = null
    )

    @Before
    fun setUp() {
        vehicleRepository = mockk()
        reviewRepository = mockk()
        viewModel = NewReviewViewModel(vehicleRepository, reviewRepository)
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
        coVerify(exactly = 0) { reviewRepository.createReview(any(), any(), any()) }
    }

    @Test
    fun `save al crear con comentario en blanco envia null y marca guardado`() = runTest {
        coEvery { reviewRepository.createReview("1", 4, null) } returns Result.success(review(id = "nueva", rating = 4))
        viewModel.updateRating(4)
        viewModel.updateExperience("   ")

        viewModel.save(vehicleId = "1")
        advanceUntilIdle()

        coVerify(exactly = 1) { reviewRepository.createReview("1", 4, null) }
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
        coVerify(exactly = 0) { reviewRepository.createReview(any(), any(), any()) }
        assertThat(viewModel.uiState.value.isSaved).isTrue()
    }

    @Test
    fun `save si el repositorio falla muestra su mensaje y no marca guardado`() = runTest {
        coEvery { reviewRepository.createReview(any(), any(), any()) } returns
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
