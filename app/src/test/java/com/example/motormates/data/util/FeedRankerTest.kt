package com.example.motormates.data.util

import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.util.OnboardingTestData.MEDELLIN
import com.example.motormates.data.util.OnboardingTestData.NOW
import com.example.motormates.data.util.OnboardingTestData.review
import com.example.motormates.data.util.OnboardingTestData.vehicle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedRankerTest {

    private val porsche = vehicle("1", "Porsche", "Deportivo")
    private val landCruiser = vehicle("2", "Toyota", "SUV")
    private val hilux = vehicle("3", "Toyota", "Pickup")
    private val tesla = vehicle("4", "Tesla", "Eléctrico")
    private val vehicles = listOf(porsche, landCruiser, hilux, tesla)

    private fun rank(preferences: UserPreferences, reviews: List<com.example.motormates.data.model.Review> = emptyList()) =
        FeedRanker.rankVehicles(vehicles, reviews, preferences, NOW)

    @Test
    fun `sin preferencias ni resenias el orden es el del id y no hay motivos`() {
        val ranked = rank(UserPreferences())

        assertThat(ranked.map { it.vehicle.id }).containsExactly("1", "2", "3", "4").inOrder()
        assertThat(ranked.map { it.reason }).containsExactly(null, null, null, null)
    }

    @Test
    fun `sin preferencias manda la popularidad`() {
        val reviews = listOf(
            review("a", vehicleId = "4", rating = 5),
            review("b", vehicleId = "4", rating = 5),
            review("c", vehicleId = "2", rating = 3)
        )

        val ranked = rank(UserPreferences(), reviews)

        assertThat(ranked.first().vehicle.id).isEqualTo("4")
        assertThat(ranked.first().reason).isEqualTo(RecommendationReason.Popular)
        assertThat(ranked.first().isPersonal).isFalse()
    }

    @Test
    fun `la categoria favorita pesa mas que la marca`() {
        val preferences = UserPreferences(categories = listOf("SUV"), brands = listOf("Tesla"))

        val ranked = rank(preferences)

        assertThat(ranked[0].vehicle).isEqualTo(landCruiser)
        assertThat(ranked[0].score).isEqualTo(FeedRanker.CATEGORY_POINTS)
        assertThat(ranked[0].reason).isEqualTo(RecommendationReason.FavoriteCategory("SUV"))
        assertThat(ranked[1].vehicle).isEqualTo(tesla)
        assertThat(ranked[1].reason).isEqualTo(RecommendationReason.FavoriteBrand("Tesla"))
    }

    @Test
    fun `categoria y marca suman y el motivo es la categoria`() {
        val preferences = UserPreferences(categories = listOf("Pickup"), brands = listOf("Toyota"))

        val ranked = rank(preferences)

        assertThat(ranked[0].vehicle).isEqualTo(hilux)
        assertThat(ranked[0].score).isEqualTo(FeedRanker.CATEGORY_POINTS + FeedRanker.BRAND_POINTS)
        assertThat(ranked[0].reason).isEqualTo(RecommendationReason.FavoriteCategory("Pickup"))
        // El Land Cruiser solo coincide en marca.
        assertThat(ranked[1].vehicle).isEqualTo(landCruiser)
    }

    @Test
    fun `compara sin tildes ni mayusculas`() {
        val preferences = UserPreferences(categories = listOf("electrico"), brands = listOf("PORSCHE"))

        val ranked = rank(preferences)

        assertThat(ranked.map { it.vehicle.id }.take(2)).containsExactly("4", "1").inOrder()
    }

    @Test
    fun `resenias recientes en la ciudad dan un punto y el motivo de ciudad`() {
        val reviews = listOf(
            review("bog", vehicleId = "3", hoursAgo = 5),
            review("med", vehicleId = "1", hoursAgo = 5, location = MEDELLIN)
        )

        val ranked = rank(UserPreferences(city = "Bogotá"), reviews)

        assertThat(ranked[0].vehicle).isEqualTo(hilux)
        assertThat(ranked[0].reason).isEqualTo(RecommendationReason.PopularInCity("Bogotá"))
        // La de Medellín no cuenta para quien vive en Bogotá: solo es popular.
        assertThat(ranked.first { it.vehicle == porsche }.reason).isEqualTo(RecommendationReason.Popular)
    }

    @Test
    fun `una resenia vieja en la ciudad no cuenta`() {
        val reviews = listOf(review("vieja", vehicleId = "3", hoursAgo = 100))

        val ranked = rank(UserPreferences(city = "Bogotá"), reviews)

        assertThat(ranked.first { it.vehicle == hilux }.reason).isEqualTo(RecommendationReason.Popular)
    }

    @Test
    fun `para ti solo lleva coincidencias personales y excluye el carro propio`() {
        val mine = vehicle("5", "Mazda", "SUV", ownerId = "yo")
        val ranked = FeedRanker.rankVehicles(
            vehicles + mine,
            listOf(review("a", vehicleId = "4", rating = 5)),
            UserPreferences(categories = listOf("SUV")),
            NOW
        )

        val forYou = FeedRanker.forYou(ranked, UserPreferences(categories = listOf("SUV")), excludedOwnerId = "yo")

        assertThat(forYou.map { it.vehicle.id }).containsExactly("2")
    }

    @Test
    fun `para ti queda vacio sin gustos declarados`() {
        val ranked = rank(UserPreferences(), listOf(review("a", vehicleId = "1")))

        assertThat(FeedRanker.forYou(ranked, UserPreferences(), excludedOwnerId = null)).isEmpty()
    }

    @Test
    fun `para ti no pasa del tope`() {
        val many = (1..10).map { vehicle("$it", "Kia", "SUV") }
        val preferences = UserPreferences(categories = listOf("SUV"))
        val ranked = FeedRanker.rankVehicles(many, emptyList(), preferences, NOW)

        assertThat(FeedRanker.forYou(ranked, preferences, null)).hasSize(FeedRanker.FOR_YOU_SIZE)
    }

    @Test
    fun `las resenias destacadas priorizan los gustos y excluyen a los seguidos`() {
        val reviews = listOf(
            review("deportivo", vehicleId = "1", userId = "ana", hoursAgo = 30),
            review("suv-vieja", vehicleId = "2", userId = "beto", hoursAgo = 30),
            review("suv-reciente", vehicleId = "2", userId = "caro", hoursAgo = 2, likes = 20),
            review("suv-seguido", vehicleId = "2", userId = "seguido", hoursAgo = 1, likes = 50)
        )

        val ranked = FeedRanker.rankReviews(
            reviews,
            vehicles,
            UserPreferences(categories = listOf("SUV")),
            NOW,
            excludedUserIds = setOf("seguido")
        )

        assertThat(ranked.map { it.id }).containsExactly("suv-reciente", "suv-vieja", "deportivo").inOrder()
    }

    @Test
    fun `los motivos se leen en espanol`() {
        assertThat(RecommendationReason.FavoriteCategory("SUV").vehicleLabel()).isEqualTo("Porque te gustan los SUV")
        assertThat(RecommendationReason.FavoriteCategory("Clásico").vehicleLabel()).isEqualTo("Porque te gustan los clásicos")
        assertThat(RecommendationReason.FavoriteBrand("Mazda").userLabel()).isEqualTo("Reseña carros Mazda")
        assertThat(RecommendationReason.PopularInCity("Cali").userLabel()).isEqualTo("Activo en Cali")
    }
}
