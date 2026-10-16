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
        fun createRoute(vehicleId: String) = "comments/$vehicleId"
    }

    object VehicleDetail : ScreenRoute("vehicle/{vehicleId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        fun createRoute(vehicleId: String) = "vehicle/$vehicleId"
    }

    /**
     * Sirve para crear y para editar. reviewId viaja como query param
     * opcional: si no viene (null) significa "nueva reseña".
     * Solo viaja el id, no el texto: el ViewModel vuelve a consultar las
     * reseñas del vehículo y busca la suya, así se evita tener que
     * codificar en la URL un comentario escrito por el usuario.
     */
    object NewReview : ScreenRoute("newReview/{vehicleId}?reviewId={reviewId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        const val ARG_REVIEW_ID = "reviewId"
        fun createRoute(vehicleId: String, reviewId: String? = null) =
            if (reviewId == null) "newReview/$vehicleId" else "newReview/$vehicleId?reviewId=$reviewId"
    }

    object Story : ScreenRoute("story/{storyIndex}") {
        const val ARG_STORY_INDEX = "storyIndex"
        fun createRoute(storyIndex: Int) = "story/$storyIndex"
    }

    /**
     * Perfil de otro usuario, registrado en Firestore (uid de Auth o id de
     * un usuario semilla). Se llega desde el buscador o desde una reseña.
     */
    object PublicProfileFirebase : ScreenRoute("publicProfileFirebase/{uid}") {
        const val ARG_UID = "uid"
        fun createRoute(uid: String) = "publicProfileFirebase/$uid"
    }
}
