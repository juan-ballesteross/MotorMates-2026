package com.example.motormates.ui.alerts

import com.example.motormates.data.model.NotificationUi
import com.example.motormates.data.model.mockNotifications

data class AlertsUiState(
    val notifications: List<NotificationUi> = mockNotifications
)
