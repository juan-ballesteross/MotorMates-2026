package com.example.motormates.data.source

import com.example.motormates.data.dto.FollowDto
import com.example.motormates.data.dto.UserDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

interface FollowDataSource {
    suspend fun followUser(followerUid: String, targetUid: String)
    suspend fun unfollowUser(followerUid: String, targetUid: String)
    suspend fun isFollowing(followerUid: String, targetUid: String): Boolean
    suspend fun getFollowers(uid: String): List<UserDto>
    suspend fun getFollowing(uid: String): List<UserDto>
}

class FirestoreFollowDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : FollowDataSource {

    private fun userRef(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    /**
     * Una transacción con las 4 escrituras: el seguidor queda anotado en
     * "followers" del otro, el otro queda anotado en "following" del
     * seguidor, y se suma 1 a cada contador. Si ya lo seguía, no hace nada.
     */
    override suspend fun followUser(followerUid: String, targetUid: String) {
        val followerDoc = userRef(targetUid).collection(FOLLOWERS_COLLECTION).document(followerUid)
        val followingDoc = userRef(followerUid).collection(FOLLOWING_COLLECTION).document(targetUid)

        firestore.runTransaction { transaction ->
            val alreadyFollowing = transaction.get(followerDoc).exists()

            if (!alreadyFollowing) {
                val now = System.currentTimeMillis()
                transaction.set(followerDoc, FollowDto(id = followerUid, createdAt = now))
                transaction.set(followingDoc, FollowDto(id = targetUid, createdAt = now))
                transaction.update(userRef(targetUid), "followersCount", FieldValue.increment(1))
                transaction.update(userRef(followerUid), "followingCount", FieldValue.increment(1))
            }

            null
        }.await()
    }

    override suspend fun unfollowUser(followerUid: String, targetUid: String) {
        val followerDoc = userRef(targetUid).collection(FOLLOWERS_COLLECTION).document(followerUid)
        val followingDoc = userRef(followerUid).collection(FOLLOWING_COLLECTION).document(targetUid)

        firestore.runTransaction { transaction ->
            val wasFollowing = transaction.get(followerDoc).exists()

            if (wasFollowing) {
                transaction.delete(followerDoc)
                transaction.delete(followingDoc)
                transaction.update(userRef(targetUid), "followersCount", FieldValue.increment(-1))
                transaction.update(userRef(followerUid), "followingCount", FieldValue.increment(-1))
            }

            null
        }.await()
    }

    override suspend fun isFollowing(followerUid: String, targetUid: String): Boolean =
        userRef(targetUid).collection(FOLLOWERS_COLLECTION).document(followerUid)
            .get().await().exists()

    override suspend fun getFollowers(uid: String): List<UserDto> =
        getUsers(userRef(uid).collection(FOLLOWERS_COLLECTION).get().await().documents.map { it.id })

    override suspend fun getFollowing(uid: String): List<UserDto> =
        getUsers(userRef(uid).collection(FOLLOWING_COLLECTION).get().await().documents.map { it.id })

    /** Una lectura por uid (no un whereIn): la lista de la subcolección ya trae los ids. */
    private suspend fun getUsers(uids: List<String>): List<UserDto> = coroutineScope {
        uids
            .map { uid -> async { userRef(uid).get().await().toObject(UserDto::class.java) } }
            .mapNotNull { it.await() }
    }
}
