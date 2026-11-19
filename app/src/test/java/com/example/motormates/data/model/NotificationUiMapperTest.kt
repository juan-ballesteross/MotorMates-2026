package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El mapper de dominio a UI de la campana. Arma el mensaje en español, así que
 * lo que se prueba es que cada tipo de evento diga lo suyo y que la ausencia
 * de título no deje la frase a medias.
 */
class NotificationUiMapperTest {

    @Test
    fun `me interesa nombra el evento cuando hay titulo`() {
        val ui = notificacion(
            type = NotificationType.EVENT_INTERESTED,
            eventTitle = "Encuentro en el 93"
        ).toNotificationUi()

        assertThat(ui.message).isEqualTo("le interesa tu evento Encuentro en el 93")
    }

    /** Sin título el mensaje tiene que seguir siendo una frase completa. */
    @Test
    fun `me interesa sin titulo no deja el mensaje a medias`() {
        val ui = notificacion(
            type = NotificationType.EVENT_INTERESTED,
            eventTitle = null
        ).toNotificationUi()

        assertThat(ui.message).isEqualTo("le interesa tu evento")
    }

    @Test
    fun `voy a ir nombra el evento cuando hay titulo`() {
        val ui = notificacion(
            type = NotificationType.EVENT_GOING,
            eventTitle = "Pique en Tocancipá"
        ).toNotificationUi()

        assertThat(ui.message).isEqualTo("va a ir a tu evento Pique en Tocancipá")
    }

    /** Lo que necesita la navegación a EventDetail desde la campana. */
    @Test
    fun `el eventId llega a la fila de la campana`() {
        val ui = notificacion(type = NotificationType.EVENT_GOING).toNotificationUi()

        assertThat(ui.eventId).isEqualTo("evt-1")
    }

    /** Un título en blanco se trata como ausente, no se pega al mensaje. */
    @Test
    fun `un titulo en blanco se trata como ausente`() {
        val ui = notificacion(
            type = NotificationType.EVENT_GOING,
            eventTitle = "   "
        ).toNotificationUi()

        assertThat(ui.message).isEqualTo("va a ir a tu evento")
    }

    // ---------- Fixtures ----------

    private fun notificacion(
        type: NotificationType,
        eventTitle: String? = "Encuentro en el 93"
    ) = Notification(
        id = "n1",
        type = type,
        actorId = "uid-beto",
        actorName = "Beto Marca",
        actorAvatarUrl = null,
        vehicleId = null,
        vehicleBrand = null,
        vehicleModel = null,
        vehicleImageUrl = null,
        reviewId = null,
        eventId = "evt-1",
        eventTitle = eventTitle,
        createdAt = null
    )
}
