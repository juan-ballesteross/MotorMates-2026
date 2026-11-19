package com.example.motormates.data.source

import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.StoryFirestoreDto
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.FollowActivity
import com.example.motormates.data.model.LikeActivity
import com.example.motormates.data.model.StoryActivity
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Lo que hizo un usuario y que no se puede sacar de los repositorios que ya
 * existen: los likes que dio, a quién siguió (con fecha) y sus historias.
 * Sus reseñas y sus carros salen de ReviewRepository y VehicleRepository.
 */
interface ActivityDataSource {
    suspend fun getReviewLikesBy(uid: String, limit: Int): List<LikeActivity>
    suspend fun getFollowingBy(uid: String): List<FollowActivity>
    suspend fun getStoriesBy(uid: String): List<StoryActivity>
}

class FirestoreActivityDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : ActivityDataSource {

    /**
     * Cada like vive dentro de su reseña (reviews/{id}/likes/{uid}), así que
     * "todos mis likes" es una consulta de grupo de colecciones. Requiere una
     * regla para {path=**}/likes y un índice de grupo en likes.userId (ver
     * firestore.rules y firestore.indexes.json); sin ellos esta consulta falla
     * y el repositorio sigue sin los likes.
     *
     * Las historias también tienen una subcolección "likes": se descartan
     * mirando de qué colección cuelga cada like.
     */
    override suspend fun getReviewLikesBy(uid: String, limit: Int): List<LikeActivity> = coroutineScope {
        val likes = firestore.collectionGroup(LIKES_COLLECTION)
            .whereEqualTo("userId", uid)
            .get().await()
            .documents
            .mapNotNull { doc ->
                val reviewRef = doc.reference.parent.parent ?: return@mapNotNull null
                if (reviewRef.parent.id != REVIEWS_COLLECTION) return@mapNotNull null
                reviewRef to (doc.getLong("createdAt") ?: 0L)
            }
            .sortedByDescending { (_, likedAt) -> likedAt }
            .take(limit)

        // La reseña trae copiados el autor y el carro: una lectura por like.
        likes.map { (reviewRef, likedAt) ->
            async {
                val review = runCatching { reviewRef.get().await().toObject(ReviewFirestoreDto::class.java) }
                    .getOrNull() ?: return@async null
                LikeActivity(
                    reviewId = reviewRef.id,
                    vehicleId = review.vehicleId,
                    vehicleName = review.vehicle?.let { "${it.brand} ${it.model}".trim() }.orEmpty(),
                    authorId = review.userId,
                    authorName = review.user?.fullName.orEmpty(),
                    likedAt = likedAt
                )
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getFollowingBy(uid: String): List<FollowActivity> = coroutineScope {
        val following = firestore.collection(USERS_COLLECTION).document(uid)
            .collection(FOLLOWING_COLLECTION)
            .get().await()
            .documents
            .map { it.id to (it.getLong("createdAt") ?: 0L) }

        following.map { (targetUid, followedAt) ->
            async {
                val user = runCatching {
                    firestore.collection(USERS_COLLECTION).document(targetUid).get().await()
                        .toObject(UserDto::class.java)
                }.getOrNull()
                FollowActivity(
                    userId = targetUid,
                    userName = user?.fullName?.ifBlank { null } ?: "Usuario",
                    followedAt = followedAt
                )
            }
        }.awaitAll()
    }

    override suspend fun getStoriesBy(uid: String): List<StoryActivity> =
        firestore.collection(STORIES_COLLECTION)
            .whereEqualTo("userId", uid)
            .get().await()
            .toObjects(StoryFirestoreDto::class.java)
            .map { StoryActivity(storyId = it.id, imageUrl = it.imageUrl.ifBlank { null }, createdAt = it.createdAt) }
}

/**
 * Con el backend propio no hay de dónde sacar estas tres cosas (no tiene
 * endpoints para ellas): la actividad queda con reseñas y carros.
 */
class EmptyActivityDataSource @Inject constructor() : ActivityDataSource {
    override suspend fun getReviewLikesBy(uid: String, limit: Int): List<LikeActivity> = emptyList()
    override suspend fun getFollowingBy(uid: String): List<FollowActivity> = emptyList()
    override suspend fun getStoriesBy(uid: String): List<StoryActivity> = emptyList()
}
