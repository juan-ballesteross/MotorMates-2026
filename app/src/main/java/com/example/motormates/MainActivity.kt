package com.example.motormates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.register.RegisterScreen
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.user.UserScreen

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

private enum class MainDestination { FEED, SEARCH, PROFILE }

@Composable
fun MotorMatesApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var authDestination by remember { mutableStateOf(AuthDestination.LOGIN) }
    var mainDestination by remember { mutableStateOf(MainDestination.FEED) }

    when {
        isLoggedIn && mainDestination == MainDestination.SEARCH -> SearchScreen(
            onFeedClick = { mainDestination = MainDestination.FEED },
            onProfileClick = { mainDestination = MainDestination.PROFILE }
        )
        isLoggedIn && mainDestination == MainDestination.PROFILE -> UserScreen(
            onFeedClick = { mainDestination = MainDestination.FEED },
            onExploreClick = { mainDestination = MainDestination.SEARCH }
        )
        isLoggedIn -> FeedScreen(
            onExploreClick = { mainDestination = MainDestination.SEARCH },
            onProfileClick = { mainDestination = MainDestination.PROFILE }
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
