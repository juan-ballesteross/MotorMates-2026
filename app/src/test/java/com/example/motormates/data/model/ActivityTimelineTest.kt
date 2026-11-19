package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/** La línea de tiempo de la pestaña Actividad: qué entra, en qué orden y cómo se lee. */
class ActivityTimelineTest {

    private val now = Instant.parse("2026-11-16T20:00:00Z")
    private fun hoursAgo(h: Long) = now.minusSeconds(h * 3600).toEpochMilli()

    private fun review(id: String, vehicleId: String, at: Long?, rating: Int = 4) = Review(
        id = id, rating = rating, comment = "ok", userId = "me", vehicleId = vehicleId,
        createdAt = at?.let { Instant.ofEpochMilli(it).toString() }, author = null,
        vehicle = ReviewVehicleRef(vehicleId, "Porsche", "911 GT3")
    )

    private fun vehicle(id: String, owner: String?, at: Long?) = Vehicle(
        id = id, brand = "Mazda", model = "MX-5", year = 2020, category = "Deportivo",
        imageUrl = "https://$id.jpg", potencia = null, aceleracion = null, velocidadMaxima = null,
        traccion = null, ownerId = owner, createdAt = at
    )

    @Test
    fun `mezcla todo y ordena de lo mas reciente a lo mas viejo`() {
        val timeline = buildActivityTimeline(
            myUid = "me",
            reviews = listOf(review("r1", "p1", hoursAgo(5))),
            vehicles = listOf(vehicle("v1", "me", hoursAgo(1))),
            likes = listOf(LikeActivity("r9", "p1", "Porsche 911 GT3", "sofia", "Sofía", hoursAgo(30))),
            follows = listOf(FollowActivity("ivan", "Iván", hoursAgo(3))),
            stories = listOf(StoryActivity("s1", "https://s1.jpg", hoursAgo(200)))
        )

        assertThat(timeline.map { it.type }).containsExactly(
            ActivityType.VEHICLE,
            ActivityType.FOLLOW,
            ActivityType.REVIEW,
            ActivityType.LIKE,
            ActivityType.STORY
        ).inOrder()
    }

    @Test
    fun `sin fecha no entra y los carros de otros tampoco`() {
        val timeline = buildActivityTimeline(
            myUid = "me",
            reviews = listOf(review("r1", "p1", at = null)),
            vehicles = listOf(vehicle("semilla", "me", at = null), vehicle("ajeno", "otro", hoursAgo(1))),
            likes = emptyList(),
            follows = emptyList(),
            stories = emptyList()
        )

        assertThat(timeline).isEmpty()
    }

    @Test
    fun `la resena usa la foto del carro si la conoce`() {
        val timeline = buildActivityTimeline(
            myUid = "me",
            reviews = listOf(review("r1", "v1", hoursAgo(2))),
            vehicles = listOf(vehicle("v1", "otro", null)),
            likes = emptyList(),
            follows = emptyList(),
            stories = emptyList()
        )

        assertThat(timeline.single().imageUrl).isEqualTo("https://v1.jpg")
    }

    @Test
    fun `respeta el limite`() {
        val reviews = (1..80).map { review("r$it", "p1", hoursAgo(it.toLong())) }

        val timeline = buildActivityTimeline("me", reviews, emptyList(), emptyList(), emptyList(), emptyList(), limit = 50)

        assertThat(timeline).hasSize(50)
        assertThat(timeline.first().id).isEqualTo("review_r1")
    }

    // ---------- Textos ----------

    private fun textOf(item: ActivityItem) = item.toActivityUi(now).parts.joinToString("") { it.text }

    @Test
    fun `cada tipo se lee como una frase y resalta los nombres`() {
        val like = ActivityItem("like_r9", ActivityType.LIKE, hoursAgo(1), vehicleId = "p1",
            vehicleName = "Porsche 911 GT3", otherUserId = "sofia", otherUserName = "Sofía")

        assertThat(textOf(like)).isEqualTo("Te gustó la reseña de Sofía sobre el Porsche 911 GT3")
        assertThat(like.toActivityUi(now).parts.filter { it.bold }.map { it.text })
            .containsExactly("Sofía", "Porsche 911 GT3")

        assertThat(textOf(ActivityItem("v", ActivityType.VEHICLE, hoursAgo(1), vehicleName = "Mazda MX-5")))
            .isEqualTo("Publicaste tu Mazda MX-5")
        assertThat(textOf(ActivityItem("f", ActivityType.FOLLOW, hoursAgo(1), otherUserName = "Iván")))
            .isEqualTo("Empezaste a seguir a Iván")
        assertThat(textOf(ActivityItem("r", ActivityType.REVIEW, hoursAgo(1), vehicleName = "Jeep Wrangler")))
            .isEqualTo("Reseñaste el Jeep Wrangler")
        assertThat(textOf(ActivityItem("s", ActivityType.STORY, hoursAgo(1)))).isEqualTo("Subiste una historia")
    }

    @Test
    fun `se agrupa en hoy esta semana y antes`() {
        assertThat(ActivityItem("a", ActivityType.STORY, hoursAgo(2)).toActivityUi(now).section).isEqualTo("HOY")
        assertThat(ActivityItem("b", ActivityType.STORY, hoursAgo(50)).toActivityUi(now).section).isEqualTo("ESTA SEMANA")
        assertThat(ActivityItem("c", ActivityType.STORY, hoursAgo(24 * 10)).toActivityUi(now).section).isEqualTo("ANTES")
    }

    @Test
    fun `solo las resenas muestran estrellas`() {
        val review = ActivityItem("r", ActivityType.REVIEW, hoursAgo(1), rating = 4)
        val like = ActivityItem("l", ActivityType.LIKE, hoursAgo(1), rating = 4)

        assertThat(review.toActivityUi(now).rating).isEqualTo(4)
        assertThat(like.toActivityUi(now).rating).isNull()
    }
}
