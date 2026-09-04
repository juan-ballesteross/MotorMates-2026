package com.example.motormates.ui.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Ya NO tiene su propio Scaffold — el único Scaffold de la app vive en
 * MainActivity.kt (MotorMatesApp), igual que Feed y Search.
 */
@Composable
fun UserScreen(
    onEditProfileClick: () -> Unit = {},
    viewModel: UserViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UserScreenContent(
        profile = uiState.profile,
        cars = uiState.cars,
        selectedTab = uiState.selectedTab,
        onSelectTab = viewModel::updateSelectedTab,
        onEditProfileClick = onEditProfileClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun UserScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.PROFILE) }
        ) { innerPadding ->
            UserScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
