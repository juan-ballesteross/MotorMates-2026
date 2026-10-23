package com.example.motormates.data.remote.datasource

import com.example.motormates.data.dto.NotificationDto
import com.example.motormates.data.remote.dto.CreateNotificationRequest
import com.example.motormates.data.remote.pollingFlow
import com.example.motormates.data.remote.service.UserApiService
import com.example.motormates.data.source.NotificationDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class NotificationRetrofitDataSource @Inject constructor(
    private val service: UserApiService
) : NotificationDataSource {

    /** No te notificas a ti mismo (el backend también lo ignora). */
    override suspend fun createNotification(
        recipientUid: String,
        type: String,
        actorId: String,
        vehicleId: String?,
        reviewId: String?
    ) {
        if (recipientUid == actorId) return
        service.createNotification(
            recipientUid,
            CreateNotificationRequest(type = type, actorId = actorId, vehicleId = vehicleId, reviewId = reviewId)
        )
    }

    /** El backend ya las devuelve de la más reciente a la más vieja. */
    override suspend fun getNotifications(uid: String): List<NotificationDto> =
        service.getNotifications(uid)

    override fun observeNotifications(uid: String): Flow<List<NotificationDto>> =
        pollingFlow { service.getNotifications(uid) }
}
