package com.example.motormates.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.motormates.data.model.NotificationType
import com.example.motormates.ui.alerts.AlertsScreen
import com.example.motormates.ui.chat.ChatListScreen
import com.example.motormates.ui.chat.ChatScreen
import com.example.motormates.ui.comments.CommentsScreen
import com.example.motormates.ui.compare.CompareScreen
import com.example.motormates.ui.editProfile.EditProfileScreen
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.followlist.FollowListScreen
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
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    // Tocar el autor de una reseña: si soy yo, va a mi propio perfil en vez
    // del perfil público, mismo criterio que ya usaba la pantalla de historias.
    fun openAuthor(userId: String) {
        if (userId == sessionViewModel.currentUserId()) {
            navController.navigate(ScreenRoute.Profile.route)
        } else {
            navController.navigate(ScreenRoute.PublicProfileFirebase.createRoute(userId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Splash.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220)) { it / 10 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutHorizontally(animationSpec = tween(180)) { it / 10 }
        }
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
                },
                onAuthorClick = ::openAuthor,
                onAlertsClick = { navController.navigate(ScreenRoute.Alerts.route) }
            )
        }
        composable(ScreenRoute.Explore.route) {
            SearchScreen(
                onCarClick = { car ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(car.id))
                },
                onUserClick = { user ->
                    navController.navigate(ScreenRoute.PublicProfileFirebase.createRoute(user.uid))
                }
            )
        }
        composable(ScreenRoute.Post.route) {
            PostScreen(
                onCancelClick = { navController.popBackStack() },
                // Recién publicado se abre su detalle; "atrás" ya no vuelve al formulario.
                onPublished = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId)) {
                        popUpTo(ScreenRoute.Post.route) { inclusive = true }
                    }
                }
            )
        }
        composable(ScreenRoute.Profile.route) { entry ->
            val reviewsChanged by entry.reviewsChangedState()
            val profileChanged by entry.profileChangedState()

            UserScreen(
                onEditProfileClick = { navController.navigate(ScreenRoute.EditProfile.route) },
                onVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                },
                onEditReviewClick = { vehicleId, reviewId ->
                    navController.navigate(ScreenRoute.NewReview.createRoute(vehicleId, reviewId))
                },
                onFollowersClick = {
                    sessionViewModel.currentUserId()?.let { uid ->
                        navController.navigate(
                            ScreenRoute.FollowList.createRoute(uid, ScreenRoute.FollowList.MODE_FOLLOWERS)
                        )
                    }
                },
                onFollowingClick = {
                    sessionViewModel.currentUserId()?.let { uid ->
                        navController.navigate(
                            ScreenRoute.FollowList.createRoute(uid, ScreenRoute.FollowList.MODE_FOLLOWING)
                        )
                    }
                },
                onLoggedOut = {
                    navController.navigate(ScreenRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onPublishVehicleClick = { navController.navigate(ScreenRoute.Post.route) },
                onUserClick = ::openAuthor,
                reloadSignal = reviewsChanged || profileChanged,
                onReloadHandled = {
                    entry.clearReviewsChanged()
                    entry.clearProfileChanged()
                }
            )
        }
        composable(ScreenRoute.Alerts.route) {
            AlertsScreen(
                onNotificationClick = { notification ->
                    val vehicleId = notification.vehicleId
                    when {
                        notification.type == NotificationType.MESSAGE ->
                            navController.navigate(ScreenRoute.Chat.createRoute(notification.actorId))
                        // Reseñas y likes: se abre el carro del que se trata.
                        vehicleId != null ->
                            navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                        notification.type == NotificationType.FOLLOW -> openAuthor(notification.actorId)
                    }
                }
            )
        }
        composable(ScreenRoute.EditProfile.route) {
            EditProfileScreen(
                onCloseClick = { navController.popBackStack() },
                onAddVehicleClick = { navController.navigate(ScreenRoute.Post.route) },
                onEditVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.EditVehicle.createRoute(vehicleId))
                },
                onSaveClick = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(RESULT_PROFILE_CHANGED, true)
                    navController.popBackStack()
                },
                onLoggedOut = {
                    navController.navigate(ScreenRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = ScreenRoute.EditVehicle.route,
            arguments = listOf(
                navArgument(ScreenRoute.EditVehicle.ARG_VEHICLE_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getString(ScreenRoute.EditVehicle.ARG_VEHICLE_ID).orEmpty()
            // Guardar o eliminar vuelve a la pantalla anterior (Editar perfil),
            // que recarga el garaje al volver a primer plano.
            PostScreen(
                vehicleId = vehicleId,
                onCancelClick = { navController.popBackStack() },
                onPublished = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(
            route = ScreenRoute.VehicleDetail.route,
            arguments = listOf(
                navArgument(ScreenRoute.VehicleDetail.ARG_VEHICLE_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getString(ScreenRoute.VehicleDetail.ARG_VEHICLE_ID).orEmpty()
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
                onCompareClick = {
                    navController.navigate(ScreenRoute.Compare.createRoute(vehicleId))
                },
                reloadSignal = reviewsChanged,
                onReloadHandled = { entry.clearReviewsChanged() }
            )
        }
        composable(
            route = ScreenRoute.Compare.route,
            arguments = listOf(
                navArgument(ScreenRoute.Compare.ARG_LEFT_ID) { type = NavType.StringType },
                navArgument(ScreenRoute.Compare.ARG_RIGHT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(ScreenRoute.Compare.ARG_MINE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val leftId = entry.arguments?.getString(ScreenRoute.Compare.ARG_LEFT_ID).orEmpty()
            val rightId = entry.arguments?.getString(ScreenRoute.Compare.ARG_RIGHT_ID)
            val compareWithMine = entry.arguments?.getBoolean(ScreenRoute.Compare.ARG_MINE) ?: false

            CompareScreen(
                leftVehicleId = leftId,
                rightVehicleId = rightId,
                compareWithMine = compareWithMine,
                onBackClick = { navController.popBackStack() },
                onVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                }
            )
        }
        composable(
            route = ScreenRoute.Comments.route,
            arguments = listOf(
                navArgument(ScreenRoute.Comments.ARG_VEHICLE_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getString(ScreenRoute.Comments.ARG_VEHICLE_ID).orEmpty()
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
                navArgument(ScreenRoute.NewReview.ARG_VEHICLE_ID) { type = NavType.StringType },
                navArgument(ScreenRoute.NewReview.ARG_REVIEW_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getString(ScreenRoute.NewReview.ARG_VEHICLE_ID).orEmpty()
            val reviewId = entry.arguments?.getString(ScreenRoute.NewReview.ARG_REVIEW_ID)

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
                onUserClick = ::openAuthor
            )
        }
        composable(
            route = ScreenRoute.PublicProfileFirebase.route,
            arguments = listOf(
                navArgument(ScreenRoute.PublicProfileFirebase.ARG_UID) { type = NavType.StringType }
            )
        ) { entry ->
            val uid = entry.arguments?.getString(ScreenRoute.PublicProfileFirebase.ARG_UID)
            PublicProfileScreen(
                firebaseUid = uid,
                onBackClick = { navController.popBackStack() },
                onVehicleClick = { vehicleId ->
                    navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                },
                onFollowersClick = { targetUid ->
                    navController.navigate(
                        ScreenRoute.FollowList.createRoute(targetUid, ScreenRoute.FollowList.MODE_FOLLOWERS)
                    )
                },
                onFollowingClick = { targetUid ->
                    navController.navigate(
                        ScreenRoute.FollowList.createRoute(targetUid, ScreenRoute.FollowList.MODE_FOLLOWING)
                    )
                },
                onMessageClick = { otherUid ->
                    navController.navigate(ScreenRoute.Chat.createRoute(otherUid))
                },
                onCompareWithMineClick = { vehicleId ->
                    navController.navigate(ScreenRoute.Compare.createRoute(vehicleId, compareWithMine = true))
                }
            )
        }
        composable(
            route = ScreenRoute.FollowList.route,
            arguments = listOf(
                navArgument(ScreenRoute.FollowList.ARG_UID) { type = NavType.StringType },
                navArgument(ScreenRoute.FollowList.ARG_MODE) { type = NavType.StringType }
            )
        ) { entry ->
            val uid = entry.arguments?.getString(ScreenRoute.FollowList.ARG_UID).orEmpty()
            val mode = entry.arguments?.getString(ScreenRoute.FollowList.ARG_MODE)

            FollowListScreen(
                uid = uid,
                isFollowers = mode == ScreenRoute.FollowList.MODE_FOLLOWERS,
                onBackClick = { navController.popBackStack() },
                onUserClick = { user -> openAuthor(user.uid) }
            )
        }
        composable(ScreenRoute.ChatList.route) {
            ChatListScreen(
                onBackClick = { navController.popBackStack() },
                onChatClick = { otherUid ->
                    navController.navigate(ScreenRoute.Chat.createRoute(otherUid))
                }
            )
        }
        composable(
            route = ScreenRoute.Chat.route,
            arguments = listOf(
                navArgument(ScreenRoute.Chat.ARG_OTHER_UID) { type = NavType.StringType }
            )
        ) { entry ->
            val otherUid = entry.arguments?.getString(ScreenRoute.Chat.ARG_OTHER_UID).orEmpty()
            ChatScreen(
                otherUid = otherUid,
                onBackClick = { navController.popBackStack() },
                onProfileClick = { uid -> navController.navigate(ScreenRoute.PublicProfileFirebase.createRoute(uid)) }
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

@Composable
private fun NavBackStackEntry.profileChangedState(): State<Boolean> =
    savedStateHandle.getStateFlow(RESULT_PROFILE_CHANGED, false).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearProfileChanged() {
    savedStateHandle[RESULT_PROFILE_CHANGED] = false
}
