package com.example.motormates.data.source

import com.example.motormates.data.dto.NotificationDto
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

interface NotificationDataSource {
    suspend fun createNotification(
        recipientUid: String,
        type: String,
        actorId: String,
        vehicleId: String?,
        reviewId: String?
    )

    suspend fun getNotifications(uid: String): List<NotificationDto>
}

class FirestoreNotificationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationDataSource {

    private fun notifications(uid: String) =
        firestore.collection(USERS_COLLECTION).document(uid).collection(NOTIFICATIONS_COLLECTION)

    override suspend fun createNotification(
        recipientUid: String,
        type: String,
        actorId: String,
        vehicleId: String?,
        reviewId: String?
    ) {
        if (recipientUid == actorId) return

        notifications(recipientUid).add(
            NotificationDto(
                type = type,
                actorId = actorId,
                vehicleId = vehicleId,
                reviewId = reviewId,
                createdAt = System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun getNotifications(uid: String): List<NotificationDto> =
        notifications(uid).get().await()
            .toObjects(NotificationDto::class.java)
            .sortedByDescending { it.createdAt }
}
