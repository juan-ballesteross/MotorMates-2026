package com.example.motormates.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.motormates.data.mock.FeedMocks
import com.example.motormates.data.model.CURRENT_USER_ID
import com.example.motormates.ui.alerts.AlertsScreen
import com.example.motormates.ui.comments.CommentsScreen
import com.example.motormates.ui.editProfile.EditProfileScreen
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.post.PostScreen
import com.example.motormates.ui.publicProfile.PublicProfileScreen
import com.example.motormates.ui.register.RegisterScreen
import com.example.motormates.ui.review.NewReviewScreen
import com.example.motormates.ui.search.SearchScreen
import com.example.motormates.ui.splash.SplashScreen
import com.example.motormates.ui.story.StoryScreen
import com.example.motormates.ui.user.UserScreen
import com.example.motormates.ui.vehicleDetail.VehicleDetailScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Tocar el autor de una reseña: si soy yo, va a mi propio perfil en vez
    // del perfil público, mismo criterio que ya usaba la pantalla de historias.
    fun openAuthor(userId: Int) {
        if (userId == CURRENT_USER_ID) {
            navController.navigate(ScreenRoute.Profile.route)
        } else {
            navController.navigate(ScreenRoute.PublicProfile.createRoute(userId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Splash.route,
        modifier = modifier
    ) {
        composable(ScreenRoute.Splash.route) {
            SplashScreen(
                onSplashFinished = { isUserLoggedIn ->
                    val destination =
                        if (isUserLoggedIn) ScreenRoute.Feed.route else ScreenRoute.Login.route
                    navController.navigate(destination) {
                        popUpTo(ScreenRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }
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
            FeedScreen(
                onVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                },
                onStoryClick = { index ->
                    navController.navigate(ScreenRoute.Story.createRoute(index))
                }
            )
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
                onEditProfileClick = { navController.navigate(ScreenRoute.EditProfile.route) },
                onVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                }
            )
        }
        composable(ScreenRoute.Alerts.route) {
            AlertsScreen()
        }
        composable(ScreenRoute.EditProfile.route) {
            EditProfileScreen(
                onCloseClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(ScreenRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = ScreenRoute.VehicleDetail.route,
            arguments = listOf(
                navArgument(ScreenRoute.VehicleDetail.ARG_VEHICLE_ID) { type = NavType.IntType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getInt(ScreenRoute.VehicleDetail.ARG_VEHICLE_ID) ?: -1
            val reviewsChanged by entry.reviewsChangedState()

            VehicleDetailScreen(
                vehicleId = vehicleId,
                onBackClick = { navController.popBackStack() },
                onWriteReviewClick = {
                    navController.navigate(ScreenRoute.NewReview.createRoute(vehicleId))
                },
                onSeeAllReviewsClick = {
                    navController.navigate(ScreenRoute.Comments.createRoute(vehicleId))
                },
                onAuthorClick = { userId -> openAuthor(userId) },
                onEditReviewClick = { reviewId ->
                    navController.navigate(ScreenRoute.NewReview.createRoute(vehicleId, reviewId))
                },
                reloadSignal = reviewsChanged,
                onReloadHandled = { entry.clearReviewsChanged() }
            )
        }
        composable(
            route = ScreenRoute.Comments.route,
            arguments = listOf(
                navArgument(ScreenRoute.Comments.ARG_VEHICLE_ID) { type = NavType.IntType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getInt(ScreenRoute.Comments.ARG_VEHICLE_ID) ?: -1
            val reviewsChanged by entry.reviewsChangedState()

            CommentsScreen(
                vehicleId = vehicleId,
                onBackClick = { navController.popBackStack() },
                onAuthorClick = { userId -> openAuthor(userId) },
                onEditReviewClick = { reviewId ->
                    navController.navigate(ScreenRoute.NewReview.createRoute(vehicleId, reviewId))
                },
                reloadSignal = reviewsChanged,
                onReloadHandled = { entry.clearReviewsChanged() }
            )
        }
        composable(
            route = ScreenRoute.NewReview.route,
            arguments = listOf(
                navArgument(ScreenRoute.NewReview.ARG_VEHICLE_ID) { type = NavType.IntType },
                navArgument(ScreenRoute.NewReview.ARG_REVIEW_ID) {
                    type = NavType.IntType
                    defaultValue = ScreenRoute.NewReview.NO_REVIEW_ID
                }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getInt(ScreenRoute.NewReview.ARG_VEHICLE_ID) ?: -1
            val reviewId = entry.arguments?.getInt(ScreenRoute.NewReview.ARG_REVIEW_ID)
                ?: ScreenRoute.NewReview.NO_REVIEW_ID

            NewReviewScreen(
                vehicleId = vehicleId,
                reviewId = reviewId,
                onCloseClick = { navController.popBackStack() },
                onSaved = {
                    // Se avisa a la pantalla anterior que su lista cambió:
                    // su LaunchedEffect(vehicleId) no se vuelve a disparar al volver.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(RESULT_REVIEWS_CHANGED, true)
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = ScreenRoute.Story.route,
            arguments = listOf(
                navArgument(ScreenRoute.Story.ARG_STORY_INDEX) { type = NavType.IntType }
            )
        ) { entry ->
            val storyIndex = entry.arguments?.getInt(ScreenRoute.Story.ARG_STORY_INDEX) ?: 0
            StoryScreen(
                storyIndex = storyIndex,
                onCloseClick = { navController.popBackStack() },
                onUserClick = { index ->
                    FeedMocks.sampleStories.getOrNull(index)?.let { openAuthor(it.userId) }
                }
            )
        }
        composable(
            route = ScreenRoute.PublicProfile.route,
            arguments = listOf(
                navArgument(ScreenRoute.PublicProfile.ARG_USER_ID) { type = NavType.IntType }
            )
        ) { entry ->
            val userId = entry.arguments?.getInt(ScreenRoute.PublicProfile.ARG_USER_ID) ?: -1
            PublicProfileScreen(
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                }
            )
        }
    }
}

/**
 * Lee la bandera que la pantalla de nueva/editar reseña dejó en esta entrada
 * del back stack antes de cerrarse. Es el patrón de resultado de Navigation
 * Compose: no hace falta estado compartido entre pantallas.
 */
@Composable
private fun NavBackStackEntry.reviewsChangedState(): State<Boolean> =
    savedStateHandle.getStateFlow(RESULT_REVIEWS_CHANGED, false).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearReviewsChanged() {
    savedStateHandle[RESULT_REVIEWS_CHANGED] = false
}
