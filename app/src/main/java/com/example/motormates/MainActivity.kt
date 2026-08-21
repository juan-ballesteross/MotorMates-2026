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
import androidx.compose.ui.graphics.Color
import com.example.motormates.ui.carDetail.CarDetailScreen
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.post.PostScreen
import com.example.motormates.ui.register.RegisterScreen
import com.example.motormates.ui.review.NewReviewScreen
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.theme.MotorMatesBackground
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

private enum class MainDestination { FEED, SEARCH, POST, CAR_DETAIL, NEW_REVIEW, PROFILE }

/**
 * Único Scaffold de toda la app (requisito del sprint). El bottomBar
 * solo se muestra cuando el usuario ya inició sesión y depende de
 * mainDestination, que se controla aquí. Cada pantalla (FeedScreen,
 * SearchScreen, UserScreen) ya no trae su propio Scaffold, solo su
 * contenido.
 */
@Composable
fun MotorMatesApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var authDestination by remember { mutableStateOf(AuthDestination.LOGIN) }
    var mainDestination by remember { mutableStateOf(MainDestination.FEED) }
    var carSelected by remember { mutableStateOf(false) }

    val showBottomBar = isLoggedIn &&
            mainDestination != MainDestination.CAR_DETAIL &&
            mainDestination != MainDestination.NEW_REVIEW

    Scaffold(
        containerColor = if (mainDestination == MainDestination.NEW_REVIEW) {
            Color.Black
        } else {
            MotorMatesBackground
        },
        bottomBar = {
            if (showBottomBar) {
                MainBottomNavBar(
                    selected = when (mainDestination) {
                        MainDestination.FEED, MainDestination.POST -> MainBottomDestination.FEED
                        MainDestination.PROFILE -> MainBottomDestination.PROFILE
                        else -> MainBottomDestination.EXPLORE
                    },
                    onFeedClick = { mainDestination = MainDestination.FEED },
                    onExploreClick = { mainDestination = MainDestination.SEARCH },
                    onPublishClick = { mainDestination = MainDestination.POST },
                    onProfileClick = { mainDestination = MainDestination.PROFILE }
                )
            }
        }
    ) { innerPadding ->
        when {
            isLoggedIn && mainDestination == MainDestination.NEW_REVIEW && carSelected -> {
                NewReviewScreen(
                    onCloseClick = { mainDestination = MainDestination.CAR_DETAIL },
                    onPublishClick = { _, _, _, _ ->
                        mainDestination = MainDestination.CAR_DETAIL
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            isLoggedIn && mainDestination == MainDestination.CAR_DETAIL && carSelected -> {
                CarDetailScreen(
                    onBackClick = {
                        carSelected = false
                        mainDestination = MainDestination.SEARCH
                    },
                    onWriteReviewClick = { mainDestination = MainDestination.NEW_REVIEW },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            isLoggedIn && mainDestination == MainDestination.POST -> PostScreen(
                onCancelClick = { mainDestination = MainDestination.FEED },
                onPublishClick = { _ -> mainDestination = MainDestination.FEED },
                modifier = Modifier.padding(innerPadding)
            )
            isLoggedIn && mainDestination == MainDestination.SEARCH -> SearchScreen(
                onCarClick = { _ ->
                    carSelected = true
                    mainDestination = MainDestination.CAR_DETAIL
                },
                modifier = Modifier.padding(innerPadding)
            )
            isLoggedIn && mainDestination == MainDestination.PROFILE -> UserScreen(
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
