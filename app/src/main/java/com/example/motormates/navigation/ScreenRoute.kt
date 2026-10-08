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

    /** Lista completa de reseñas de un vehículo. */
    object Comments : ScreenRoute("comments/{vehicleId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        fun createRoute(vehicleId: Int) = "comments/$vehicleId"
    }

    object VehicleDetail : ScreenRoute("vehicle/{vehicleId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        fun createRoute(vehicleId: Int) = "vehicle/$vehicleId"
    }

    /**
     * Sirve para crear y para editar. reviewId viaja como query param
     * opcional: -1 (su valor por defecto) significa "nueva reseña".
     * Solo viaja el id, no el texto: el ViewModel vuelve a consultar las
     * reseñas del vehículo y busca la suya, así se evita tener que
     * codificar en la URL un comentario escrito por el usuario.
     */
    object NewReview : ScreenRoute("newReview/{vehicleId}?reviewId={reviewId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        const val ARG_REVIEW_ID = "reviewId"
        const val NO_REVIEW_ID = -1

        fun createRoute(vehicleId: Int, reviewId: Int = NO_REVIEW_ID) =
            "newReview/$vehicleId?reviewId=$reviewId"
    }

    object Story : ScreenRoute("story/{storyIndex}") {
        const val ARG_STORY_INDEX = "storyIndex"
        fun createRoute(storyIndex: Int) = "story/$storyIndex"
    }

    /** Perfil de otro usuario: el id es el de la tabla users del backend. */
    object PublicProfile : ScreenRoute("publicProfile/{userId}") {
        const val ARG_USER_ID = "userId"
        fun createRoute(userId: Int) = "publicProfile/$userId"
    }
}
