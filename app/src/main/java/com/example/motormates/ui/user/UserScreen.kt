package com.example.motormates.ui.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.user.model.ProfileTab
import com.example.motormates.ui.user.model.sampleUserCars
import com.example.motormates.ui.user.model.sampleUserProfile

/**
 * Pantalla de perfil de usuario: portada, avatar, datos del perfil,
 * estadísticas, pestañas y garaje de autos, siguiendo el mismo patrón
 * de Scaffold + MainBottomNavBar que Feed y Search.
 */
@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    onFeedClick: () -> Unit = {},
    onExploreClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(ProfileTab.REVIEWS) }

    Scaffold(
        modifier = modifier,
        containerColor = MotorMatesBackground,
        bottomBar = {
            MainBottomNavBar(
                selected = MainBottomDestination.PROFILE,
                onFeedClick = onFeedClick,
                onExploreClick = onExploreClick
            )
        }
    ) { innerPadding ->
        UserScreenContent(
            profile = sampleUserProfile,
            cars = sampleUserCars,
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
            onEditProfileClick = {},
            modifier = Modifier.padding(innerPadding)
        )
    }
}
