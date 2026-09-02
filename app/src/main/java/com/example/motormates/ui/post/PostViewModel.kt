package com.example.motormates.ui.post

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Un MutableStateFlow privado + StateFlow público por cada campo,
 * mismo patrón que LoginViewModel.
 */
class PostViewModel : ViewModel() {

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption

    private val _taggedVehicle = MutableStateFlow<String?>(null)
    val taggedVehicle: StateFlow<String?> = _taggedVehicle

    private val _location = MutableStateFlow<String?>(null)
    val location: StateFlow<String?> = _location

    fun updateCaption(input: String) {
        _caption.value = input
    }

    /**
     * TODO: todavía no existe una pantalla real para elegir vehículo,
     * así que solo se alterna un valor de ejemplo. Reemplazar por
     * navegación real cuando esa pantalla exista.
     */
    fun toggleTaggedVehicle() {
        _taggedVehicle.value = if (_taggedVehicle.value == null) "Porsche 911 GT3" else null
    }

    /**
     * TODO: todavía no existe una pantalla real para elegir ubicación,
     * así que solo se alterna un valor de ejemplo. Reemplazar por
     * navegación real cuando esa pantalla exista.
     */
    fun toggleLocation() {
        _location.value = if (_location.value == null) "Bogotá, Colombia" else null
    }
}
