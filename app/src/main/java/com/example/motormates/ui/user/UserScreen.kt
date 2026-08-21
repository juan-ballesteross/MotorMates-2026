package com.example.motormates.ui.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
