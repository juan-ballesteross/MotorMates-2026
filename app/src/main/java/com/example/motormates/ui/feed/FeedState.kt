package com.example.motormates.ui.feed

import com.example.motormates.data.model.FeedReviewUi
import com.example.motormates.data.model.FeedTab
import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.model.StoryUser

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
    val followingErrorMessage: String? = null
)
