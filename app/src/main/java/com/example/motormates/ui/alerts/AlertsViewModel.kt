package com.example.motormates.ui.alerts

import androidx.lifecycle.ViewModel
import com.example.motormates.data.model.mockNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AlertsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState

    init {
        _uiState.update { it.copy(notifications = mockNotifications) }
    }
}
