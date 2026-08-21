package com.example.motormates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
<<<<<<< HEAD
=======
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
>>>>>>> origin/master
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
<<<<<<< HEAD
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.register.RegisterScreen
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.user.UserScreen
=======
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
import com.example.motormates.ui.review.toCarDetailUi
import com.example.motormates.ui.review.toReviewCarSummary
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.search.model.CarListing
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesTheme
>>>>>>> origin/master

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

<<<<<<< HEAD
private enum class MainDestination { FEED, SEARCH, PROFILE }

=======
private enum class MainDestination { FEED, SEARCH, POST, CAR_DETAIL, NEW_REVIEW }

/**
 * Único Scaffold de toda la app (requisito del sprint). El bottomBar
 * solo se muestra cuando el usuario ya inició sesión y depende de
 * mainDestination, que se controla aquí. Cada pantalla (FeedScreen,
 * SearchScreen) ya no trae su propio Scaffold, solo su contenido.
 */
>>>>>>> origin/master
@Composable
fun MotorMatesApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var authDestination by remember { mutableStateOf(AuthDestination.LOGIN) }
    var mainDestination by remember { mutableStateOf(MainDestination.FEED) }
<<<<<<< HEAD

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
=======
    var selectedCar by remember { mutableStateOf<CarListing?>(null) }

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
                        else -> MainBottomDestination.EXPLORE
                    },
                    onFeedClick = { mainDestination = MainDestination.FEED },
                    onExploreClick = { mainDestination = MainDestination.SEARCH },
                    onPublishClick = { mainDestination = MainDestination.POST }
                )
            }
        }
    ) { innerPadding ->
        when {
            isLoggedIn && mainDestination == MainDestination.NEW_REVIEW && selectedCar != null -> {
                NewReviewScreen(
                    car = selectedCar!!.toReviewCarSummary(),
                    onCloseClick = { mainDestination = MainDestination.CAR_DETAIL },
                    onPublishClick = { _, _, _, _ ->
                        mainDestination = MainDestination.CAR_DETAIL
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            isLoggedIn && mainDestination == MainDestination.CAR_DETAIL && selectedCar != null -> {
                CarDetailScreen(
                    car = selectedCar!!.toCarDetailUi(),
                    onBackClick = {
                        selectedCar = null
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
                onCarClick = { car ->
                    selectedCar = car
                    mainDestination = MainDestination.CAR_DETAIL
                },
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
>>>>>>> origin/master
    }
}
