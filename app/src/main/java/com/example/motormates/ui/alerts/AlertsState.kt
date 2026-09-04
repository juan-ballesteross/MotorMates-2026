package com.example.motormates.ui.alerts

import com.example.motormates.data.model.NotificationUi

data class AlertsUiState(
    val notifications: List<NotificationUi> = emptyList()
)
