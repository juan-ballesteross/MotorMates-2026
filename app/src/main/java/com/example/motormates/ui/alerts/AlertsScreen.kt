package com.example.motormates.ui.alerts


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.data.model.NotificationUi
import com.example.motormates.data.model.mockNotifications
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de la pantalla. onNotificationClick recibe el id de
 * la notificación — cuando se conecte la navegación real, permite abrir
 * el detalle específico (la reseña, el perfil del usuario que siguió, etc.).
 */
@Composable
fun AlertsScreen(
    notifications: List<NotificationUi> = mockNotifications,
    onNotificationClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AlertsScreenContent(
        notifications = notifications,
        onNotificationClick = onNotificationClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun AlertsScreenPreview() {
    MotorMatesTheme {
        AlertsScreen()
    }
}
