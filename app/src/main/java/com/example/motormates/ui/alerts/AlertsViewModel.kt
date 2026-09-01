package com.example.motormates.ui.alerts

import androidx.lifecycle.ViewModel
import com.example.motormates.data.model.NotificationUi
import com.example.motormates.data.model.mockNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Por ahora esta pantalla es de solo lectura (no hay acción que
 * modifique una notificación todavía, como marcar como leída), así
 * que el ViewModel solo expone la lista. Cuando se agregue esa acción,
 * su función va aquí (ej. markAsReadButtonPress(id)).
 */
class AlertsViewModel : ViewModel() {

    private val _notifications = MutableStateFlow(mockNotifications)
    val notifications: StateFlow<List<NotificationUi>> = _notifications
}