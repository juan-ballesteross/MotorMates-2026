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
     * Comparador de dos vehículos. rightId es opcional: desde el detalle solo
     * se conoce el primero y la pantalla abre el selector para el segundo.
     * mine = true ("Comparar con el mío", desde un perfil): la pantalla busca
     * por su cuenta cuál es el carro del usuario para ponerlo a la derecha.
     */
    object Compare : ScreenRoute("compare/{leftId}?rightId={rightId}&mine={mine}") {
        const val ARG_LEFT_ID = "leftId"
        const val ARG_RIGHT_ID = "rightId"
        const val ARG_MINE = "mine"
        fun createRoute(leftId: String, rightId: String? = null, compareWithMine: Boolean = false): String {
            val params = buildList {
                rightId?.let { add("rightId=$it") }
                if (compareWithMine) add("mine=true")
            }
            return if (params.isEmpty()) "compare/$leftId" else "compare/$leftId?" + params.joinToString("&")
        }
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

    /** Crear y editar un evento en la misma pantalla: eventId null = crear. */
    object NewEvent : ScreenRoute("newEvent?eventId={eventId}") {
        const val ARG_EVENT_ID = "eventId"
        fun createRoute(eventId: String? = null) =
            if (eventId == null) "newEvent" else "newEvent?eventId=$eventId"
    }

    object EventDetail : ScreenRoute("event/{eventId}") {
        const val ARG_EVENT_ID = "eventId"
        fun createRoute(eventId: String) = "event/$eventId"
    }

    /**
     * Elegir en el mapa dónde será un evento.
     *
     * lat y lng sí pueden viajar por la ruta, al contrario que el resto de
     * los datos del formulario: son números y no texto escrito por el
     * usuario, y sirven para abrir el mapa donde ya estaba el punto al
     * editar. Van como texto y no como Float porque NavType.FloatType
     * pierde precisión. El resultado vuelve por el savedStateHandle
     * (ver RESULT_PICKED_LAT).
     */
    object PickLocation : ScreenRoute("pickLocation?lat={lat}&lng={lng}") {
        const val ARG_LAT = "lat"
        const val ARG_LNG = "lng"
        fun createRoute(lat: Double? = null, lng: Double? = null) =
            if (lat == null || lng == null) "pickLocation" else "pickLocation?lat=$lat&lng=$lng"
    }

    /** El mismo formulario de Publicar, prellenado con un vehículo propio. */
    object EditVehicle : ScreenRoute("editVehicle/{vehicleId}") {
        const val ARG_VEHICLE_ID = "vehicleId"
        fun createRoute(vehicleId: String) = "editVehicle/$vehicleId"
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

    object FollowList : ScreenRoute("followList/{uid}/{mode}") {
        const val ARG_UID = "uid"
        const val ARG_MODE = "mode"
        const val MODE_FOLLOWERS = "followers"
        const val MODE_FOLLOWING = "following"
        fun createRoute(uid: String, mode: String) = "followList/$uid/$mode"
    }

    object ChatList : ScreenRoute("chatList")

    object Chat : ScreenRoute("chat/{otherUid}") {
        const val ARG_OTHER_UID = "otherUid"
        fun createRoute(otherUid: String) = "chat/$otherUid"
    }
}
