package com.example.motormates.ui.post

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PostViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState

    fun updateCaption(input: String) {
        _uiState.update { it.copy(caption = input) }
    }

    fun toggleTaggedVehicle() {
        _uiState.update { current ->
            current.copy(taggedVehicle = if (current.taggedVehicle == null) "Porsche 911 GT3" else null)
        }
    }

    fun toggleLocation() {
        _uiState.update { current ->
            current.copy(location = if (current.location == null) "Bogotá, Colombia" else null)
        }
    }
}
