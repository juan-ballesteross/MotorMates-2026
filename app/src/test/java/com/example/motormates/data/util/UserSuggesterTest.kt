package com.example.motormates.data.util

import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.util.OnboardingTestData.MEDELLIN
import com.example.motormates.data.util.OnboardingTestData.review
import com.example.motormates.data.util.OnboardingTestData.vehicle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UserSuggesterTest {

    private val vehicles = listOf(
        vehicle("1", "Porsche", "Deportivo"),
        vehicle("2", "Toyota", "SUV"),
        vehicle("3", "Jeep", "SUV")
    )

    @Test
    fun `primero quienes resenian lo que te gusta, aunque otros tengan mas likes`() {
        val reviews = listOf(
            review("a", vehicleId = "2", userId = "fan-suv", likes = 1),
            review("b", vehicleId = "3", userId = "fan-suv", likes = 1),
            review("c", vehicleId = "1", userId = "popular", likes = 40)
        )

        val suggestions = UserSuggester.suggest(
            reviews, vehicles, UserPreferences(categories = listOf("SUV")),
            currentUserId = "yo", followingIds = emptySet()
        )

        assertThat(suggestions.map { it.userId }).containsExactly("fan-suv", "popular").inOrder()
        assertThat(suggestions[0].reason).isEqualTo(RecommendationReason.FavoriteCategory("SUV"))
        assertThat(suggestions[0].reviewsCount).isEqualTo(2)
        assertThat(suggestions[1].reason).isEqualTo(RecommendationReason.Popular)
    }

    @Test
    fun `excluye al usuario actual y a quienes ya sigue`() {
        val reviews = listOf(
            review("a", vehicleId = "2", userId = "yo"),
            review("b", vehicleId = "2", userId = "seguido"),
            review("c", vehicleId = "2", userId = "nuevo")
        )

        val suggestions = UserSuggester.suggest(
            reviews, vehicles, UserPreferences(categories = listOf("SUV")),
            currentUserId = "yo", followingIds = setOf("seguido")
        )

        assertThat(suggestions.map { it.userId }).containsExactly("nuevo")
    }

    @Test
    fun `la marca y la ciudad tambien cuentan`() {
        val reviews = listOf(
            review("a", vehicleId = "1", userId = "fan-porsche", location = MEDELLIN),
            review("b", vehicleId = "3", userId = "de-bogota")
        )

        val suggestions = UserSuggester.suggest(
            reviews, vehicles, UserPreferences(brands = listOf("Porsche"), city = "Bogotá"),
            currentUserId = null, followingIds = emptySet()
        )

        assertThat(suggestions.map { it.userId }).containsExactly("fan-porsche", "de-bogota").inOrder()
        assertThat(suggestions[0].reason).isEqualTo(RecommendationReason.FavoriteBrand("Porsche"))
        assertThat(suggestions[1].reason).isEqualTo(RecommendationReason.PopularInCity("Bogotá"))
    }

    @Test
    fun `respeta el limite y usa el nombre copiado en la resenia`() {
        val reviews = (1..12).map { review("r$it", vehicleId = "2", userId = "u$it", authorName = "Persona $it") }

        val suggestions = UserSuggester.suggest(
            reviews, vehicles, UserPreferences(), currentUserId = null, followingIds = emptySet(), limit = 3
        )

        assertThat(suggestions).hasSize(3)
        assertThat(suggestions.first().name).startsWith("Persona ")
    }

    @Test
    fun `sin resenias no hay sugerencias`() {
        assertThat(
            UserSuggester.suggest(emptyList(), vehicles, UserPreferences(), null, emptySet())
        ).isEmpty()
    }
}
