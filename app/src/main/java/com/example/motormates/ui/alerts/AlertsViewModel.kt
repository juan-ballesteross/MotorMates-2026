package com.example.motormates.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toNotificationUi
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        val userId = authRepository.getCurrentUser()?.id
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false, notifications = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            notificationRepository.getNotifications(userId).fold(
                onSuccess = { notifications ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            notifications = notifications.map { notification -> notification.toNotificationUi() }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar las notificaciones"
                        )
                    }
                }
            )
        }
    }
}
