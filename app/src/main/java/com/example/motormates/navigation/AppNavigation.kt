package com.example.motormates.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.toReviewCarSummary
import com.example.motormates.ui.alerts.AlertsScreen
import com.example.motormates.ui.editProfile.EditProfileScreen
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.post.PostScreen
import com.example.motormates.ui.register.RegisterScreen
import com.example.motormates.ui.review.NewReviewScreen
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.user.UserScreen
import com.example.motormates.ui.vehicleDetail.VehicleDetailScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Login.route,
        modifier = modifier
    ) {
        composable(ScreenRoute.Login.route) {
            LoginScreen(
                onLoginClick = {
                    navController.navigate(ScreenRoute.Feed.route) {
                        popUpTo(ScreenRoute.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(ScreenRoute.Register.route) }
            )
        }
        composable(ScreenRoute.Register.route) {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterClick = {
                    navController.navigate(ScreenRoute.Feed.route) {
                        popUpTo(ScreenRoute.Login.route) { inclusive = true }
                    }
                },
                onLoginClick = { navController.popBackStack() }
            )
        }
        composable(ScreenRoute.Feed.route) {
            FeedScreen()
        }
        composable(ScreenRoute.Explore.route) {
            SearchScreen(
                onCarClick = { car ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(car.id))
                }
            )
        }
        composable(ScreenRoute.Post.route) {
            PostScreen(
                onCancelClick = { navController.popBackStack() },
                onPublishClick = { _ ->
                    navController.navigate(ScreenRoute.Feed.route) {
                        popUpTo(ScreenRoute.Feed.route) { inclusive = true }
                    }
                }
            )
        }
        composable(ScreenRoute.Profile.route) {
            UserScreen(
                onEditProfileClick = { navController.navigate(ScreenRoute.EditProfile.route) }
            )
        }
        composable(ScreenRoute.Alerts.route) {
            AlertsScreen()
        }
        composable(ScreenRoute.EditProfile.route) {
            EditProfileScreen(
                onCloseClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() }
            )
        }
        composable(
            route = ScreenRoute.VehicleDetail.route,
            arguments = listOf(
                navArgument(ScreenRoute.VehicleDetail.ARG_VEHICLE_ID) { type = NavType.IntType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getInt(ScreenRoute.VehicleDetail.ARG_VEHICLE_ID) ?: -1
            VehicleDetailScreen(
                vehicleId = vehicleId,
                onBackClick = { navController.popBackStack() },
                onWriteReviewClick = {
                    navController.navigate(ScreenRoute.NewReview.createRoute(vehicleId))
                }
            )
        }
        composable(
            route = ScreenRoute.NewReview.route,
            arguments = listOf(
                navArgument(ScreenRoute.NewReview.ARG_VEHICLE_ID) { type = NavType.IntType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getInt(ScreenRoute.NewReview.ARG_VEHICLE_ID) ?: -1
            val car = SearchMocks.findById(vehicleId)?.toReviewCarSummary()
            if (car != null) {
                NewReviewScreen(
                    car = car,
                    onCloseClick = { navController.popBackStack() },
                    onPublishClick = { _, _, _, _ -> navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}
