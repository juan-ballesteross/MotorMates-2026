package com.example.motormates.data.model

import com.example.motormates.data.util.toEpochMillisOrNull
import com.example.motormates.data.util.toRelativeTimeEs
import java.time.Duration
import java.time.Instant

/**
 * La pestaña Actividad del perfil: todo lo que hizo el usuario, en una sola
 * línea de tiempo. Kotlin puro para probar el orden y los textos en la JVM.
 */
enum class ActivityType { REVIEW, VEHICLE, LIKE, FOLLOW, STORY }

/** Un hecho de la línea de tiempo, ya con lo necesario para pintarlo y navegar. */
data class ActivityItem(
    /** Único en la lista ("review_abc"): sirve de key en la LazyList. */
    val id: String,
    val type: ActivityType,
    val timestampMillis: Long,
    val vehicleId: String? = null,
    val vehicleName: String? = null,
    val rating: Int? = null,
    /** La otra persona: el autor de la reseña que me gustó, o a quien empecé a seguir. */
    val otherUserId: String? = null,
    val otherUserName: String? = null,
    /** Miniatura: foto del carro o de la historia. */
    val imageUrl: String? = null
)

/** Un like que di a una reseña, con lo que hace falta para contarlo. */
data class LikeActivity(
    val reviewId: String,
    val vehicleId: String,
    val vehicleName: String,
    val authorId: String,
    val authorName: String,
    val likedAt: Long
)

data class FollowActivity(
    val userId: String,
    val userName: String,
    val followedAt: Long
)

data class StoryActivity(
    val storyId: String,
    val imageUrl: String?,
    val createdAt: Long
)

const val ACTIVITY_LIMIT = 50

/**
 * Mezcla todo en una sola lista, de lo más reciente a lo más viejo. Lo que
 * no tiene fecha se descarta (no se sabría dónde ponerlo), igual que los
 * vehículos que no son míos.
 */
fun buildActivityTimeline(
    myUid: String,
    reviews: List<Review>,
    vehicles: List<Vehicle>,
    likes: List<LikeActivity>,
    follows: List<FollowActivity>,
    stories: List<StoryActivity>,
    limit: Int = ACTIVITY_LIMIT
): List<ActivityItem> {
    val imagesByVehicle = vehicles.associate { it.id to it.imageUrl }

    val reviewItems = reviews.mapNotNull { review ->
        val at = review.createdAt.toEpochMillisOrNull() ?: return@mapNotNull null
        ActivityItem(
            id = "review_${review.id}",
            type = ActivityType.REVIEW,
            timestampMillis = at,
            vehicleId = review.vehicleId,
            vehicleName = review.vehicle?.let { "${it.brand} ${it.model}".trim() },
            rating = review.rating,
            imageUrl = imagesByVehicle[review.vehicleId]
        )
    }

    val vehicleItems = vehicles
        .filter { it.ownerId == myUid }
        .mapNotNull { vehicle ->
            val at = vehicle.createdAt?.takeIf { it > 0 } ?: return@mapNotNull null
            ActivityItem(
                id = "vehicle_${vehicle.id}",
                type = ActivityType.VEHICLE,
                timestampMillis = at,
                vehicleId = vehicle.id,
                vehicleName = "${vehicle.brand} ${vehicle.model}".trim(),
                imageUrl = vehicle.imageUrl
            )
        }

    val likeItems = likes.filter { it.likedAt > 0 }.map { like ->
        ActivityItem(
            id = "like_${like.reviewId}",
            type = ActivityType.LIKE,
            timestampMillis = like.likedAt,
            vehicleId = like.vehicleId,
            vehicleName = like.vehicleName,
            otherUserId = like.authorId,
            otherUserName = like.authorName,
            imageUrl = imagesByVehicle[like.vehicleId]
        )
    }

    val followItems = follows.filter { it.followedAt > 0 && it.userId != myUid }.map { follow ->
        ActivityItem(
            id = "follow_${follow.userId}",
            type = ActivityType.FOLLOW,
            timestampMillis = follow.followedAt,
            otherUserId = follow.userId,
            otherUserName = follow.userName
        )
    }

    val storyItems = stories.filter { it.createdAt > 0 }.map { story ->
        ActivityItem(
            id = "story_${story.storyId}",
            type = ActivityType.STORY,
            timestampMillis = story.createdAt,
            imageUrl = story.imageUrl
        )
    }

    return (reviewItems + vehicleItems + likeItems + followItems + storyItems)
        .sortedByDescending { it.timestampMillis }
        .take(limit)
}

/** Un pedazo del texto de una fila; los nombres de carros y personas van en negrita. */
data class ActivityTextPart(val text: String, val bold: Boolean = false)

data class ActivityUi(
    val id: String,
    val type: ActivityType,
    val parts: List<ActivityTextPart>,
    /** Estrellas de la reseña, si es una reseña. */
    val rating: Int?,
    val timeAgo: String,
    /** "HOY", "ESTA SEMANA" o "ANTES", igual que la campana. */
    val section: String,
    val imageUrl: String?,
    /** A dónde lleva al tocarla: el carro, si hay; si no, la persona. */
    val vehicleId: String?,
    val userId: String?
)

fun ActivityItem.toActivityUi(now: Instant = Instant.now()): ActivityUi {
    val vehicle = vehicleName?.takeIf { it.isNotBlank() }
    val person = otherUserName?.takeIf { it.isNotBlank() } ?: "alguien"

    val parts = when (type) {
        ActivityType.REVIEW -> if (vehicle != null) {
            listOf(ActivityTextPart("Reseñaste el "), ActivityTextPart(vehicle, bold = true))
        } else {
            listOf(ActivityTextPart("Publicaste una reseña"))
        }
        ActivityType.VEHICLE -> listOf(
            ActivityTextPart("Publicaste tu "),
            ActivityTextPart(vehicle ?: "carro", bold = true)
        )
        ActivityType.LIKE -> buildList {
            add(ActivityTextPart("Te gustó la reseña de "))
            add(ActivityTextPart(person, bold = true))
            if (vehicle != null) {
                add(ActivityTextPart(" sobre el "))
                add(ActivityTextPart(vehicle, bold = true))
            }
        }
        ActivityType.FOLLOW -> listOf(
            ActivityTextPart("Empezaste a seguir a "),
            ActivityTextPart(person, bold = true)
        )
        ActivityType.STORY -> listOf(ActivityTextPart("Subiste una historia"))
    }

    val instant = Instant.ofEpochMilli(timestampMillis)
    val hours = Duration.between(instant, now).toHours()
    val section = when {
        hours < 24 -> "HOY"
        hours < 24 * 7 -> "ESTA SEMANA"
        else -> "ANTES"
    }

    return ActivityUi(
        id = id,
        type = type,
        parts = parts,
        rating = rating.takeIf { type == ActivityType.REVIEW },
        timeAgo = instant.toString().toRelativeTimeEs(),
        section = section,
        imageUrl = imageUrl,
        vehicleId = vehicleId,
        userId = otherUserId
    )
}
