package com.example.motormates.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.SearchCategoryKey
import com.example.motormates.data.model.toCarListing
import com.example.motormates.data.model.toUserSearchResult
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    init {
        loadCars()
        loadUsers()
    }

    /** El catálogo real (los mismos vehículos del feed), no los de prueba. */
    fun loadCars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCars = true, carsError = null) }
            vehicleRepository.getVehicles().fold(
                onSuccess = { vehicles ->
                    _uiState.update {
                        it.copy(
                            allCars = vehicles.map { vehicle -> vehicle.toCarListing() },
                            isLoadingCars = false
                        ).filterCars()
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoadingCars = false, carsError = error.message ?: "No se pudieron cargar los carros")
                    }
                }
            )
        }
    }

    /** Usuarios reales registrados con Firebase, para el buscador. */
    private fun loadUsers() {
        viewModelScope.launch {
            val currentUid = authRepository.getCurrentUser()?.id
            userRepository.getAllRegisteredUsers().onSuccess { users ->
                val results = users
                    .filter { it.id != currentUid }
                    .map { it.toUserSearchResult() }
                _uiState.update { it.copy(allUsers = results).filterUsers() }
            }
        }
    }

    fun updateSearchQuery(input: String) {
        val wasBlank = _uiState.value.searchQuery.isBlank()

        _uiState.update { current ->
            current.copy(searchQuery = input).filterCars().filterUsers()
        }

        if (wasBlank && input.isNotBlank()) {
            loadUsers()
        }
    }

    fun updateSelectedCategory(category: SearchCategoryKey) {
        _uiState.update { current ->
            current.copy(selectedCategory = category).filterCars()
        }
    }

    private fun SearchUiState.filterCars(): SearchUiState {
        val filtered = allCars.filter { car ->
            val matchesCategory = selectedCategory == SearchCategoryKey.ALL || car.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                car.brand.contains(searchQuery, ignoreCase = true) ||
                car.model.contains(searchQuery, ignoreCase = true) ||
                car.ownerName.orEmpty().contains(searchQuery, ignoreCase = true) ||
                car.categoryLabel.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        return copy(filteredCars = filtered)
    }

    /**
     * A diferencia de filterCars (que filtra sobre su propio resultado
     * anterior), esto siempre filtra desde allUsers, la lista completa.
     */
    private fun SearchUiState.filterUsers(): SearchUiState {
        val results = if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allUsers.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                    user.handle.contains(searchQuery, ignoreCase = true)
            }
        }
        return copy(userResults = results)
    }
}
