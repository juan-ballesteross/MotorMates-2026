package com.example.motormates.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.FeedMocks
import com.example.motormates.data.model.toFeedVehicleUi
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.seed.FirestoreSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val firestoreSeeder: FirestoreSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    init {
        // Las historias siguen siendo datos locales, el backend no las modela.
        _uiState.update { it.copy(stories = FeedMocks.sampleStories) }
        viewModelScope.launch {
            // Primera vez en una BD vacía: se queman los datos de prueba
            // antes de pedir los vehículos. Si falla (p. ej. reglas de
            // Firestore), igual se intenta cargar lo que haya.
            runCatching { firestoreSeeder.seedIfEmpty() }
            loadVehicles()
        }
    }

    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = vehicleRepository.getVehicles()
            _uiState.update { current ->
                result.fold(
                    onSuccess = { vehicles ->
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            vehicles = vehicles.map { it.toFeedVehicleUi() }
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar los vehículos"
                        )
                    }
                )
            }
        }
    }
}
