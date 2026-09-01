package com.example.motormates.ui.vehicleDetail

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.mockReviews
import com.example.motormates.data.model.toCarDetailUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class VehicleDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(VehicleDetailState())
    val state: StateFlow<VehicleDetailState> = _state

    // Toggle de UI independiente de cuál vehículo se muestre.
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked

    /** Busca el vehículo real en SearchMocks. Si no existe, vehicle queda null. */
    fun getVehicleById(id: Int) {
        val listing = SearchMocks.findById(id)
        _state.update { it.copy(vehicle = listing?.toCarDetailUi()) }
    }

    /**
     * TODO: las reseñas todavía no están asociadas a un vehículo específico
     * en ningún modelo — por ahora siempre se muestran las mismas 2 de mock,
     * sin importar el id. Cuando exista un modelo de reseñas por vehicleId,
     * se filtra aquí.
     */
    fun getReviews(id: Int) {
        _state.update { it.copy(reviews = mockReviews) }
    }

    fun bookmarkButtonPress() {
        _isBookmarked.value = !_isBookmarked.value
    }
}