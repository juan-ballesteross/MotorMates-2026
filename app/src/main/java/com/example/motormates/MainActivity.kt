package com.example.motormates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.register.RegisterScreen
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotorMatesTheme {
                MotorMatesApp()
            }
        }
    }
}

private enum class AuthDestination { LOGIN, REGISTER }

private enum class MainDestination { FEED, SEARCH }

/**
 * Único Scaffold de toda la app (requisito del sprint). El bottomBar
 * solo se muestra cuando el usuario ya inició sesión y depende de
 * mainDestination, que se controla aquí. Cada pantalla (FeedScreen,
 * SearchScreen) ya no trae su propio Scaffold, solo su contenido.
 */
@Composable
fun MotorMatesApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var authDestination by remember { mutableStateOf(AuthDestination.LOGIN) }
    var mainDestination by remember { mutableStateOf(MainDestination.FEED) }

    Scaffold(
        containerColor = MotorMatesBackground,
        bottomBar = {
            if (isLoggedIn) {
                MainBottomNavBar(
                    selected = if (mainDestination == MainDestination.FEED) {
                        MainBottomDestination.FEED
                    } else {
                        MainBottomDestination.EXPLORE
                    },
                    onFeedClick = { mainDestination = MainDestination.FEED },
                    onExploreClick = { mainDestination = MainDestination.SEARCH }
                )
            }
        }
    ) { innerPadding ->
        when {
            isLoggedIn && mainDestination == MainDestination.SEARCH -> SearchScreen(
                modifier = Modifier.padding(innerPadding)
            )
            isLoggedIn -> FeedScreen(
                modifier = Modifier.padding(innerPadding)
            )
            authDestination == AuthDestination.REGISTER -> RegisterScreen(
                onBackClick = { authDestination = AuthDestination.LOGIN },
                onRegisterClick = { isLoggedIn = true },
                onLoginClick = { authDestination = AuthDestination.LOGIN }
            )
            else -> LoginScreen(
                onLoginClick = { isLoggedIn = true },
                onRegisterClick = { authDestination = AuthDestination.REGISTER }
            )
        }
    }
}