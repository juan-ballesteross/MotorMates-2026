package com.example.motormates.data.viewmodel

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.CarDetailMocks
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.CarDetailUi
import com.example.motormates.data.model.ReviewUi
import com.example.motormates.data.model.toCarDetailUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VehicleDetailUiState {
    object Loading : VehicleDetailUiState
    data class Success(val car: CarDetailUi, val reviews: List<ReviewUi>) : VehicleDetailUiState
    object NotFound : VehicleDetailUiState
    data class Error(val message: String) : VehicleDetailUiState
}

class VehicleDetailViewModel(
    private val vehicleId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<VehicleDetailUiState>(VehicleDetailUiState.Loading)
    val uiState: StateFlow<VehicleDetailUiState> = _uiState.asStateFlow()

    init {
        loadVehicle()
    }

    private fun loadVehicle() {
        _uiState.value = VehicleDetailUiState.Loading
        _uiState.value = try {
            SearchMocks.findById(vehicleId)
                ?.let { listing ->
                    VehicleDetailUiState.Success(
                        car = listing.toCarDetailUi(),
                        reviews = CarDetailMocks.mockReviews
                    )
                }
                ?: VehicleDetailUiState.NotFound
        } catch (e: Exception) {
            VehicleDetailUiState.Error(e.message ?: "Ocurrió un error al cargar el vehículo")
        }
    }
}
