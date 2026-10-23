package com.example.motormates.data.source

import com.example.motormates.data.dto.NotificationDto
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /** La misma lista, pero escuchando: la campana no espera a recargar. */
    fun observeNotifications(uid: String): Flow<List<NotificationDto>>
}

class FirestoreNotificationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationDataSource {

    private fun notifications(uid: String) =
        firestore.collection(USERS_COLLECTION).document(uid).collection(NOTIFICATIONS_COLLECTION)

    /** No te notificas a ti mismo: pasa si te das like o te sigues a vos mismo. */
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

    /**
     * Mismo molde que authState en FirebaseAuthDataSource. Se ordena en
     * memoria, igual que getNotifications, para no depender de un orderBy.
     */
    override fun observeNotifications(uid: String): Flow<List<NotificationDto>> = callbackFlow {
        val registration = notifications(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val dtos = snapshot?.toObjects(NotificationDto::class.java).orEmpty()
            trySend(dtos.sortedByDescending { it.createdAt })
        }
        awaitClose { registration.remove() }
    }
}
