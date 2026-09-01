package com.example.motormates.ui.alerts


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * onNotificationClick recibe el id de la notificación — cuando se
 * conecte la navegación real, permite abrir el detalle específico
 * (la reseña, el perfil del usuario que siguió, etc.).
 */
@Composable
fun AlertsScreen(
    onNotificationClick: (String) -> Unit = {},
    viewModel: AlertsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

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
