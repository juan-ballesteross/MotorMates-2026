package com.example.motormates.navigation

sealed class ScreenRoute(val route: String) {

    object Splash : ScreenRoute("splash")
    object Login : ScreenRoute("login")
    object Register : ScreenRoute("register")

    object Feed : ScreenRoute("feed")
    object Explore : ScreenRoute("explore")
    object Post : ScreenRoute("post")
    object Profile : ScreenRoute("profile")
    object Alerts : ScreenRoute("alerts")
    object EditProfile : ScreenRoute("editProfile")
    object Comments : ScreenRoute("comments")

    object VehicleDetail : ScreenRoute("vehicle/{vehicleId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        fun createRoute(vehicleId: Int) = "vehicle/$vehicleId"
    }

    object NewReview : ScreenRoute("newReview/{vehicleId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        fun createRoute(vehicleId: Int) = "newReview/$vehicleId"
    }

    object Story : ScreenRoute("story/{storyIndex}") {
        const val ARG_STORY_INDEX = "storyIndex"
        fun createRoute(storyIndex: Int) = "story/$storyIndex"
    }

    object PublicProfile : ScreenRoute("publicProfile/{userIndex}") {
        const val ARG_USER_INDEX = "userIndex"
        fun createRoute(userIndex: Int) = "publicProfile/$userIndex"
    }
}
