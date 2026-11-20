package com.example.motormates.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
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
import com.example.motormates.ui.aiTuning.AiTuningScreen
import com.example.motormates.ui.alerts.AlertsScreen
import com.example.motormates.ui.buildDetail.BuildDetailScreen
import com.example.motormates.ui.chat.ChatListScreen
import com.example.motormates.ui.chat.ChatScreen
import com.example.motormates.ui.comments.CommentsScreen
import com.example.motormates.ui.compare.CompareScreen
import com.example.motormates.ui.editProfile.EditProfileScreen
import com.example.motormates.ui.eventDetail.EventDetailScreen
import com.example.motormates.ui.feed.FeedScreen
import com.example.motormates.ui.followlist.FollowListScreen
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.ui.login.LoginScreen
import com.example.motormates.ui.newEvent.NewEventScreen
import com.example.motormates.ui.pickLocation.PickLocationScreen
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
                },
                onAuthorClick = ::openAuthor,
                onAlertsClick = { navController.navigate(ScreenRoute.Alerts.route) },
                onEventDetailClick = { eventId ->
                    navController.navigate(ScreenRoute.EventDetail.createRoute(eventId))
                },
                onCreateEventClick = {
                    navController.navigate(ScreenRoute.NewEvent.createRoute())
                }
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
                onBuildClick = { buildId ->
                    navController.navigate(ScreenRoute.BuildDetail.createRoute(buildId))
                },
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
                // else y no un when exhaustivo: la mitad de los tipos no lleva
                // a ninguna parte, y listarlos sería siete ramas vacías.
                onNotificationClick = { notification ->
                    val vehicleId = notification.vehicleId
                    when {
                        notification.type == NotificationType.MESSAGE ->
                            navController.navigate(ScreenRoute.Chat.createRoute(notification.actorId))
                        // Los eventos van antes que la rama de vehicleId: una
                        // notificación de evento podría traer los dos ids, y el
                        // destino que corresponde es el detalle del evento.
                        notification.type == NotificationType.EVENT_INTERESTED ||
                            notification.type == NotificationType.EVENT_GOING ->
                            notification.eventId?.let {
                                navController.navigate(ScreenRoute.EventDetail.createRoute(it))
                            }
                        // Reseñas y likes (incluido REVIEW_ON_MY_CAR): se abre
                        // el carro del que se trata.
                        vehicleId != null ->
                            navController.navigate(ScreenRoute.VehicleDetail.createRoute(vehicleId))
                        notification.type == NotificationType.FOLLOW -> openAuthor(notification.actorId)
                        else -> Unit
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
                onAiTuningClick = {
                    navController.navigate(ScreenRoute.AiTuning.createRoute(vehicleId))
                },
                onBuildClick = { buildId ->
                    navController.navigate(ScreenRoute.BuildDetail.createRoute(buildId))
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
            route = ScreenRoute.AiTuning.route,
            arguments = listOf(
                navArgument(ScreenRoute.AiTuning.ARG_VEHICLE_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getString(ScreenRoute.AiTuning.ARG_VEHICLE_ID).orEmpty()
            AiTuningScreen(
                vehicleId = vehicleId,
                onBackClick = { navController.popBackStack() },
                onOpenBuild = { buildId ->
                    navController.navigate(ScreenRoute.BuildDetail.createRoute(buildId))
                }
            )
        }
        composable(
            route = ScreenRoute.BuildDetail.route,
            arguments = listOf(
                navArgument(ScreenRoute.BuildDetail.ARG_BUILD_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val buildId = entry.arguments?.getString(ScreenRoute.BuildDetail.ARG_BUILD_ID).orEmpty()
            BuildDetailScreen(
                buildId = buildId,
                onBackClick = { navController.popBackStack() },
                onAuthorClick = { userId -> openAuthor(userId) },
                onDeleted = { navController.popBackStack() }
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
            route = ScreenRoute.NewEvent.route,
            arguments = listOf(
                navArgument(ScreenRoute.NewEvent.ARG_EVENT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val eventId = entry.arguments?.getString(ScreenRoute.NewEvent.ARG_EVENT_ID)
            // El punto lo deja aquí la pantalla del mapa antes de cerrarse.
            val pickedLocation by entry.pickedLocationState()

            NewEventScreen(
                eventId = eventId,
                onCloseClick = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(RESULT_EVENTS_CHANGED, true)
                    navController.popBackStack()
                },
                onPickLocationClick = { lat, lng ->
                    navController.navigate(ScreenRoute.PickLocation.createRoute(lat, lng))
                },
                pickedLocation = pickedLocation,
                onPickedLocationHandled = { entry.clearPickedLocation() }
            )
        }
        composable(
            route = ScreenRoute.EventDetail.route,
            arguments = listOf(
                navArgument(ScreenRoute.EventDetail.ARG_EVENT_ID) { type = NavType.StringType }
            )
        ) { entry ->
            val eventId = entry.arguments?.getString(ScreenRoute.EventDetail.ARG_EVENT_ID).orEmpty()
            // Al volver de editar, el formulario deja esta bandera aquí.
            val eventsChanged by entry.eventsChangedState()

            EventDetailScreen(
                eventId = eventId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(ScreenRoute.NewEvent.createRoute(id))
                },
                onOrganizerClick = ::openAuthor,
                reloadSignal = eventsChanged,
                onReloadHandled = { entry.clearEventsChanged() }
            )
        }
        composable(
            route = ScreenRoute.PickLocation.route,
            arguments = listOf(
                navArgument(ScreenRoute.PickLocation.ARG_LAT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(ScreenRoute.PickLocation.ARG_LNG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            // Texto y luego toDoubleOrNull: NavType.FloatType perdería
            // precisión en una coordenada.
            val lat = entry.arguments?.getString(ScreenRoute.PickLocation.ARG_LAT)?.toDoubleOrNull()
            val lng = entry.arguments?.getString(ScreenRoute.PickLocation.ARG_LNG)?.toDoubleOrNull()

            PickLocationScreen(
                initialLat = lat,
                initialLng = lng,
                onBackClick = { navController.popBackStack() },
                onPicked = { location ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(RESULT_PICKED_LAT, location.lat)
                        set(RESULT_PICKED_LNG, location.lng)
                        set(RESULT_PICKED_PLACE_NAME, location.placeName)
                    }
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
                onBuildClick = { buildId ->
                    navController.navigate(ScreenRoute.BuildDetail.createRoute(buildId))
                },
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
                onBackClick = { navController.popBackStack() }
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

/**
 * El punto que dejó la pantalla de elegir en el mapa.
 *
 * Se lee como estado y se limpia tras consumirlo (ver
 * [clearPickedLocation]): si no, volver a entrar al formulario reaplicaría un
 * punto viejo sobre el que el usuario pudo haber cambiado.
 */
@Composable
private fun NavBackStackEntry.pickedLocationState(): State<ReviewLocation?> {
    val lat by savedStateHandle.getStateFlow<Double?>(RESULT_PICKED_LAT, null)
        .collectAsStateWithLifecycle()
    val lng by savedStateHandle.getStateFlow<Double?>(RESULT_PICKED_LNG, null)
        .collectAsStateWithLifecycle()
    val placeName by savedStateHandle.getStateFlow<String?>(RESULT_PICKED_PLACE_NAME, null)
        .collectAsStateWithLifecycle()

    return remember(lat, lng, placeName) {
        derivedStateOf {
            if (lat != null && lng != null) {
                ReviewLocation(lat = lat!!, lng = lng!!, placeName = placeName)
            } else {
                null
            }
        }
    }
}

private fun NavBackStackEntry.clearPickedLocation() {
    savedStateHandle[RESULT_PICKED_LAT] = null
    savedStateHandle[RESULT_PICKED_LNG] = null
    savedStateHandle[RESULT_PICKED_PLACE_NAME] = null
}

@Composable
private fun NavBackStackEntry.eventsChangedState(): State<Boolean> =
    savedStateHandle.getStateFlow(RESULT_EVENTS_CHANGED, false).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearEventsChanged() {
    savedStateHandle[RESULT_EVENTS_CHANGED] = false
}
