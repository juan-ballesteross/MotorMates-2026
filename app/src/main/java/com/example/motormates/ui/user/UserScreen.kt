package com.example.motormates.ui.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.user.model.ProfileTab
import com.example.motormates.ui.user.model.sampleUserCars
import com.example.motormates.ui.user.model.sampleUserProfile

/**
 * Ya NO tiene su propio Scaffold — el único Scaffold de la app vive en
 * MainActivity.kt (MotorMatesApp), igual que Feed y Search.
 */
@Composable
fun UserScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(ProfileTab.REVIEWS) }

    UserScreenContent(
        profile = sampleUserProfile,
        cars = sampleUserCars,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onEditProfileClick = {},
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun UserScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MotorMatesBackground,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.PROFILE) }
        ) { innerPadding ->
            UserScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
