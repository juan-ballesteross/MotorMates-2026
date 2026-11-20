package com.example.motormates.data.util

import com.example.motormates.data.model.Review
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.model.Vehicle

/** Una persona que vale la pena seguir, con el motivo para mostrarlo. */
data class SuggestedUser(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val reason: RecommendationReason,
    val reviewsCount: Int,
    val likesReceived: Int
)

/**
 * Sugerencias de a quién seguir (paso 6 del onboarding y "Siguiendo" vacía).
 *
 * Sale de las reseñas, que traen el nombre y la foto del autor copiados:
 * así no hace falta leer la colección de usuarios. Una persona es relevante
 * si reseña carros de las categorías o marcas favoritas, o si publica en la
 * misma ciudad. Entre las relevantes ganan las que más likes reciben.
 */
object UserSuggester {

    const val DEFAULT_LIMIT = 8

    fun suggest(
        reviews: List<Review>,
        vehicles: List<Vehicle>,
        preferences: UserPreferences,
        currentUserId: String?,
        followingIds: Set<String>,
        limit: Int = DEFAULT_LIMIT
    ): List<SuggestedUser> {
        val vehiclesById = vehicles.associateBy { it.id }
        val city = CityCoordinates.find(preferences.city)

        val candidates = reviews
            .filter { it.userId != currentUserId && it.userId !in followingIds }
            .groupBy { it.userId }
            .map { (userId, userReviews) ->
                // Cuántas de sus reseñas coinciden con cada gusto.
                val byCategory = preferences.categories.associateWith { category ->
                    userReviews.count { category.sameAs(vehiclesById[it.vehicleId]?.category) }
                }
                val byBrand = preferences.brands.associateWith { brand ->
                    userReviews.count { review ->
                        brand.sameAs(vehiclesById[review.vehicleId]?.brand ?: review.vehicle?.brand)
                    }
                }
                val inCity = if (city == null) 0 else userReviews.count { it.isIn(city) }

                val bestCategory = byCategory.maxByOrNull { it.value }?.takeIf { it.value > 0 }
                val bestBrand = byBrand.maxByOrNull { it.value }?.takeIf { it.value > 0 }
                val relevance = byCategory.values.sum() * 2 + byBrand.values.sum() * 2 + inCity

                val reason = when {
                    bestCategory != null -> RecommendationReason.FavoriteCategory(bestCategory.key)
                    bestBrand != null -> RecommendationReason.FavoriteBrand(bestBrand.key)
                    inCity > 0 && city != null -> RecommendationReason.PopularInCity(city.name)
                    else -> RecommendationReason.Popular
                }
                // El nombre más reciente que tenga copiado alguna de sus reseñas.
                val author = userReviews.firstNotNullOfOrNull { it.author }

                Candidate(
                    user = SuggestedUser(
                        userId = userId,
                        name = author?.fullName?.takeIf { it.isNotBlank() } ?: "Usuario de MotorMates",
                        avatarUrl = author?.avatarUrl,
                        reason = reason,
                        reviewsCount = userReviews.size,
                        likesReceived = userReviews.sumOf { it.likesCount }
                    ),
                    relevance = relevance
                )
            }

        val order = compareByDescending<Candidate> { it.relevance }
            .thenByDescending { it.user.likesReceived }
            .thenByDescending { it.user.reviewsCount }
            .thenBy { it.user.userId }

        // Primero los relevantes; si no alcanzan, se completa con los más populares.
        val relevant = candidates.filter { it.relevance > 0 }.sortedWith(order)
        val others = candidates.filter { it.relevance == 0 }.sortedWith(order)
        return (relevant + others).take(limit).map { it.user }
    }

    private data class Candidate(val user: SuggestedUser, val relevance: Int)
}
