package com.example.motormates.data.util

import com.example.motormates.data.model.Review
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.model.Vehicle
import kotlin.math.ln
import kotlin.math.min

/** Por qué se recomienda algo. La UI lo convierte en texto (ver label()). */
sealed interface RecommendationReason {
    data class FavoriteCategory(val category: String) : RecommendationReason
    data class FavoriteBrand(val brand: String) : RecommendationReason
    data class PopularInCity(val city: String) : RecommendationReason
    data object Popular : RecommendationReason
}

/** "Porque te gustan los SUV", "Es de Toyota"… para la tarjeta de un vehículo. */
fun RecommendationReason.vehicleLabel(): String = when (this) {
    is RecommendationReason.FavoriteCategory -> "Porque te gustan los ${category.categoryPlural()}"
    is RecommendationReason.FavoriteBrand -> "Es de $brand, una de tus marcas"
    is RecommendationReason.PopularInCity -> "Popular en $city"
    RecommendationReason.Popular -> "Popular en MotorMates"
}

/** "Reseña carros SUV", "Activo en Bogotá"… para una persona sugerida. */
fun RecommendationReason.userLabel(): String = when (this) {
    is RecommendationReason.FavoriteCategory -> "Reseña ${category.categoryPlural()}"
    is RecommendationReason.FavoriteBrand -> "Reseña carros $brand"
    is RecommendationReason.PopularInCity -> "Activo en $city"
    RecommendationReason.Popular -> "Popular en MotorMates"
}

private fun String.categoryPlural(): String = when (normalizedKey()) {
    "deportivo" -> "deportivos"
    "clasico" -> "clásicos"
    "electrico" -> "eléctricos"
    "pickup" -> "pickups"
    else -> this // "SUV" no cambia
}

data class RankedVehicle(
    val vehicle: Vehicle,
    val score: Double,
    /** Null si no hay nada que decir (sin gustos que coincidan y sin reseñas). */
    val reason: RecommendationReason?
) {
    /** Coincide con algo que el usuario dijo, no solo es popular. */
    val isPersonal: Boolean get() = reason != null && reason != RecommendationReason.Popular
}

/**
 * Ordena el contenido según las preferencias del onboarding.
 *
 * Kotlin puro y determinista (mismo resultado con los mismos datos), para
 * probarlo en la JVM. Se calcula en el teléfono: el catálogo es pequeño y
 * así no hacen falta índices compuestos en Firestore.
 *
 * Puntaje de un vehículo:
 *  - su categoría es una de las favoritas: +3
 *  - su marca es una de las favoritas: +2
 *  - tiene reseñas recientes publicadas en la ciudad del usuario: +1
 *  - popularidad: ln(1 + reseñas) + rating promedio / 5
 */
object FeedRanker {

    const val CATEGORY_POINTS = 3.0
    const val BRAND_POINTS = 2.0
    const val CITY_POINTS = 1.0
    const val CITY_WINDOW_MS = 72L * 60 * 60 * 1000
    const val FOR_YOU_SIZE = 5

    private const val RECENT_REVIEW_MS = 24L * 60 * 60 * 1000
    private const val RECENT_REVIEW_POINTS = 1.0
    private const val LIKES_PER_POINT = 10.0
    private const val MAX_LIKES_POINTS = 1.0

    fun rankVehicles(
        vehicles: List<Vehicle>,
        reviews: List<Review>,
        preferences: UserPreferences,
        nowMillis: Long
    ): List<RankedVehicle> {
        val city = CityCoordinates.find(preferences.city)
        val reviewsByVehicle = reviews.groupBy { it.vehicleId }

        return vehicles.map { vehicle ->
            val vehicleReviews = reviewsByVehicle[vehicle.id].orEmpty()
            val popularity = popularity(vehicleReviews)

            val categoryMatch = preferences.categories.firstOrNull { it.sameAs(vehicle.category) }
            val brandMatch = preferences.brands.firstOrNull { it.sameAs(vehicle.brand) }
            val cityMatch = city != null && vehicleReviews.any { review ->
                review.isWithin(nowMillis, CITY_WINDOW_MS) && review.isIn(city)
            }

            val score = popularity +
                (if (categoryMatch != null) CATEGORY_POINTS else 0.0) +
                (if (brandMatch != null) BRAND_POINTS else 0.0) +
                (if (cityMatch) CITY_POINTS else 0.0)

            // El motivo es el criterio que más pesó, en el mismo orden de los puntos.
            val reason = when {
                categoryMatch != null -> RecommendationReason.FavoriteCategory(vehicle.category)
                brandMatch != null -> RecommendationReason.FavoriteBrand(vehicle.brand)
                cityMatch && city != null -> RecommendationReason.PopularInCity(city.name)
                vehicleReviews.isNotEmpty() -> RecommendationReason.Popular
                else -> null
            }
            RankedVehicle(vehicle, score, reason) to popularity
        }
            .sortedWith(
                compareByDescending<Pair<RankedVehicle, Double>> { it.first.score }
                    .thenByDescending { it.second }
                    .thenBy { it.first.vehicle.id.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.first.vehicle.id }
            )
            .map { it.first }
    }

    /**
     * El carrusel "Para ti": los mejores que coinciden con algo que el
     * usuario dijo. Sin gustos declarados queda vacío y el carrusel no se
     * muestra (sería solo "lo más popular", que ya es la lista).
     */
    fun forYou(
        ranked: List<RankedVehicle>,
        preferences: UserPreferences,
        excludedOwnerId: String?
    ): List<RankedVehicle> {
        if (!preferences.hasTaste) return emptyList()
        return ranked
            .filter { it.isPersonal }
            // El carro propio no se le recomienda a su dueño.
            .filter { excludedOwnerId == null || it.vehicle.ownerId != excludedOwnerId }
            .take(FOR_YOU_SIZE)
    }

    /**
     * Reseñas destacadas para completar "Siguiendo" cuando está vacía o
     * corta: primero las de carros que coinciden con los gustos, y entre
     * ellas las más recientes y con más likes.
     */
    fun rankReviews(
        reviews: List<Review>,
        vehicles: List<Vehicle>,
        preferences: UserPreferences,
        nowMillis: Long,
        excludedUserIds: Set<String> = emptySet()
    ): List<Review> {
        val vehiclesById = vehicles.associateBy { it.id }
        val city = CityCoordinates.find(preferences.city)

        return reviews
            .filter { it.userId !in excludedUserIds }
            .map { review ->
                val vehicle = vehiclesById[review.vehicleId]
                val brand = vehicle?.brand ?: review.vehicle?.brand
                val personal =
                    (if (vehicle != null && preferences.categories.any { it.sameAs(vehicle.category) }) CATEGORY_POINTS else 0.0) +
                        (if (brand != null && preferences.brands.any { it.sameAs(brand) }) BRAND_POINTS else 0.0) +
                        (if (city != null && review.isIn(city)) CITY_POINTS else 0.0)
                val recency = if (review.isWithin(nowMillis, RECENT_REVIEW_MS)) RECENT_REVIEW_POINTS else 0.0
                val likes = min(review.likesCount / LIKES_PER_POINT, MAX_LIKES_POINTS)
                review to (personal + recency + likes)
            }
            .sortedWith(
                compareByDescending<Pair<Review, Double>> { it.second }
                    .thenByDescending { it.first.createdAt.toEpochMillisOrNull() ?: 0L }
                    .thenBy { it.first.id }
            )
            .map { it.first }
    }

    private fun popularity(reviews: List<Review>): Double {
        if (reviews.isEmpty()) return 0.0
        val average = reviews.map { it.rating }.average()
        return ln(1.0 + reviews.size) + average / 5.0
    }
}

internal fun String.sameAs(other: String?): Boolean =
    other != null && normalizedKey() == other.normalizedKey()

internal fun Review.isWithin(nowMillis: Long, windowMillis: Long): Boolean {
    val created = createdAt.toEpochMillisOrNull() ?: return false
    return nowMillis - created in 0..windowMillis
}

internal fun Review.isIn(city: City): Boolean {
    val place = location ?: return false
    return CityCoordinates.isInCity(place.lat, place.lng, city)
}
