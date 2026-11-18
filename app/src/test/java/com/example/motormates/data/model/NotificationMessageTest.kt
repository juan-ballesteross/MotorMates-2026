package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** El texto de la campana para las notificaciones de reseñas. */
class NotificationMessageTest {

    private fun notification(type: NotificationType, brand: String? = "Mazda", model: String? = "MX-5") = Notification(
        id = "n1", type = type, actorId = "uid-jose", actorName = "Jose", actorAvatarUrl = null,
        vehicleId = "v1", vehicleBrand = brand, vehicleModel = model, vehicleImageUrl = null,
        reviewId = "r1", createdAt = null
    )

    @Test
    fun `resenia en mi carro nombra el carro y deja el id para abrirlo`() {
        val ui = notification(NotificationType.REVIEW_ON_MY_CAR).toNotificationUi()

        assertThat(ui.message).isEqualTo("dejó una reseña de tu Mazda MX-5")
        assertThat(ui.vehicleId).isEqualTo("v1")
    }

    @Test
    fun `si el carro ya no existe el mensaje sigue teniendo sentido`() {
        val ui = notification(NotificationType.REVIEW_ON_MY_CAR, brand = null, model = null).toNotificationUi()

        assertThat(ui.message).isEqualTo("dejó una reseña de tu carro")
    }

    @Test
    fun `la notificacion de otros resenadores no cambia`() {
        val ui = notification(NotificationType.REVIEW).toNotificationUi()

        assertThat(ui.message).isEqualTo("también dejó una reseña en el Mazda MX-5")
    }
}
