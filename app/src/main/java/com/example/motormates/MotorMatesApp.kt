package com.example.motormates

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.motormates.navigation.AppNavigation
import com.example.motormates.navigation.ScreenRoute
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.notifications.PushTokenViewModel
import com.example.motormates.ui.theme.MotorMatesBackground

/**
 * Contenedor raíz de la app: dueño del Scaffold único (bottom bar + fondo),
 * calculado a partir de la ruta actual del backstack. El NavHost con las
 * rutas vive en AppNavigation.
 */
@Composable
fun MotorMatesApp(modifier: Modifier = Modifier) {
    // Solo se instancia: en su init se queda registrando el token de FCM
    // mientras haya sesión. Acá porque es la raíz, así vive una sola vez.
    hiltViewModel<PushTokenViewModel>()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf(
        ScreenRoute.Feed.route,
        ScreenRoute.Explore.route,
        ScreenRoute.Post.route,
        ScreenRoute.Alerts.route,
        ScreenRoute.Profile.route
    )
    val isNewReview = currentRoute == ScreenRoute.NewReview.route

    Scaffold(
        modifier = modifier,
        containerColor = if (isNewReview) Color.Black else MotorMatesBackground,
        bottomBar = {
            if (showBottomBar) {
                MainBottomNavBar(
                    selected = currentRoute.toMainBottomDestination(),
                    onFeedClick = { navController.navigateToBottomTab(ScreenRoute.Feed.route) },
                    onExploreClick = { navController.navigateToBottomTab(ScreenRoute.Explore.route) },
                    onPublishClick = { navController.navigateToBottomTab(ScreenRoute.Post.route) },
                    onAlertsClick = { navController.navigateToBottomTab(ScreenRoute.Alerts.route) },
                    onProfileClick = { navController.navigateToBottomTab(ScreenRoute.Profile.route) }
                )
            }
        }
    ) { innerPadding ->
        // innerPadding ya incluye la barra de estado y la de navegación
        // (edge-to-edge). Se marcan como consumidas para que los
        // statusBarsPadding() de los top bars y los Scaffold anidados de
        // algunas pantallas no las vuelvan a sumar (eso duplicaba el
        // espacio de arriba).
        AppNavigation(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

private fun String?.toMainBottomDestination(): MainBottomDestination = when (this) {
    ScreenRoute.Feed.route -> MainBottomDestination.FEED
    ScreenRoute.Explore.route -> MainBottomDestination.EXPLORE
    ScreenRoute.Alerts.route -> MainBottomDestination.ALERTS
    ScreenRoute.Profile.route -> MainBottomDestination.PROFILE
    else -> MainBottomDestination.FEED
}

private fun NavHostController.navigateToBottomTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
