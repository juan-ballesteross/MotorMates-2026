package com.example.motormates.ui.alerts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun AlertsScreen(
    onNotificationClick: (String) -> Unit = {},
    viewModel: AlertsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlertsScreenContent(
        notifications = uiState.notifications,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
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
