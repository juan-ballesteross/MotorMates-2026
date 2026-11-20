package com.example.motormates.ui.search

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.SearchCategoryKey
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.util.OnboardingTestData.vehicle
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * SearchViewModel: los filtros de categoría y búsqueda deben partir siempre
 * del catálogo completo. Los carros salen de VehicleRepository (el catálogo
 * real de Firestore); aquí se simula con cuatro vehículos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        val userRepository: UserRepository = mockk()
        val authRepository: AuthRepository = mockk()
        coEvery { userRepository.getAllRegisteredUsers() } returns Result.success(emptyList())
        every { authRepository.getCurrentUser() } returns null
        val vehicleRepository: VehicleRepository = mockk()
        coEvery { vehicleRepository.getVehicles() } returns Result.success(
            listOf(
                vehicle("1", "Porsche", "Deportivo"),
                vehicle("2", "Toyota", "SUV"),
                vehicle("4", "Tesla", "Eléctrico"),
                vehicle("5", "Chevrolet", "Deportivo")
            )
        )

        viewModel = SearchViewModel(userRepository, authRepository, vehicleRepository)
    }

    // ---------- Filtros ----------

    @Test
    fun `volver a Todos despues de filtrar muestra otra vez todos los carros`() = runTest {
        advanceUntilIdle()
        val total = viewModel.uiState.value.filteredCars.size

        viewModel.updateSelectedCategory(SearchCategoryKey.SPORT)
        assertThat(viewModel.uiState.value.filteredCars.size).isLessThan(total)

        viewModel.updateSelectedCategory(SearchCategoryKey.ALL)
        assertThat(viewModel.uiState.value.filteredCars).hasSize(total)
    }

    @Test
    fun `borrar la busqueda devuelve todos los carros de la categoria`() = runTest {
        advanceUntilIdle()
        viewModel.updateSearchQuery("tesla")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.filteredCars.map { it.brand }).containsExactly("Tesla")

        viewModel.updateSearchQuery("")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.filteredCars).hasSize(4)
    }
}
