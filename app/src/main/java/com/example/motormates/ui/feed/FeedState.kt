package com.example.motormates.ui.feed

import com.example.motormates.data.model.FeedReviewUi
import com.example.motormates.data.model.FeedTab
import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.model.StoryUser
import com.example.motormates.data.util.SuggestedUser

import com.example.motormates.data.model.news.NewsArticleUi

/**
 * Las dos pestañas guardan su estado por separado (cargando y error
 * propios) porque se alimentan de fuentes distintas: los vehículos de una
 * consulta puntual y las reseñas de los seguidos de un listener en vivo.
 */
data class FeedUiState(
    val selectedTab: FeedTab = FeedTab.EXPLORAR,
    val stories: List<StoryUser> = emptyList(),
    val vehicles: List<FeedVehicleUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val followingReviews: List<FeedReviewUi> = emptyList(),
    val isFollowingLoading: Boolean = false,
    val followingErrorMessage: String? = null,
    val isPostingStory: Boolean = false,
    val storyErrorMessage: String? = null,

    // ---- Personalización (onboarding) ----
    /** Carrusel "Para ti". Vacío si el usuario no ha dicho qué le gusta. */
    val forYou: List<FeedVehicleUi> = emptyList(),
    /** false: se le ofrece "Personaliza tu feed" en Explorar. */
    val hasPreferences: Boolean = true,
    /** A quién seguir, para que Siguiendo no arranque vacía. */
    val suggestions: List<SuggestedUser> = emptyList(),
    /** Reseñas de los gustos del usuario para completar Siguiendo cuando es corta. */
    val featuredReviews: List<FeedReviewUi> = emptyList(),

    // Variables de estado para Noticias
    val newsArticles: List<NewsArticleUi> = emptyList(),
    val isNewsLoading: Boolean = false,
    val newsErrorMessage: String? = null,
) {
    /** Siguiendo tiene poco: se completa con sugerencias y reseñas destacadas. */
    val showFollowingDiscovery: Boolean
        get() = !isFollowingLoading && followingErrorMessage == null &&
            followingReviews.size < FOLLOWING_DISCOVERY_THRESHOLD

    companion object {
        const val FOLLOWING_DISCOVERY_THRESHOLD = 5
    }
}
