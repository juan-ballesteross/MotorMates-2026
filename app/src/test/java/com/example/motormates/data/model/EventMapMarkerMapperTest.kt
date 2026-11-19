package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El mapeo evento -> marcador del mapa. Función pura: currentUserId y
 * nowMillis entran por parámetro justamente para poder probarla así.
 */
class EventMapMarkerMapperTest {

    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643, placeName = "Chapinero, Bogotá")
    private val inicio = 1_700_000_000_000L

    @Test
    fun `isMine es true solo para el organizador`() {
        val evento = event()

        assertThat(evento.marker(currentUserId = "uid-ana").isMine).isTrue()
        assertThat(evento.marker(currentUserId = "uid-otro").isMine).isFalse()
    }

    /** Sin sesión nadie es dueño de nada: no se deben ofrecer editar ni cancelar. */
    @Test
    fun `sin sesion isMine es false`() {
        assertThat(event().marker(currentUserId = null).isMine).isFalse()
    }

    @Test
    fun `un evento que ya termino se marca como finalizado`() {
        val yaTermino = event(startAt = inicio, endAt = inicio + 3_600_000L)

        val marcador = yaTermino.marker(nowMillis = inicio + 7_200_000L)

        assertThat(marcador.hasFinished).isTrue()
    }

    /** Sin hora de fin, el que manda es el inicio. */
    @Test
    fun `sin hora de fin se usa el inicio para saber si ya paso`() {
        val sinFin = event(startAt = inicio, endAt = null)

        assertThat(sinFin.marker(nowMillis = inicio - 1).hasFinished).isFalse()
        assertThat(sinFin.marker(nowMillis = inicio + 1).hasFinished).isTrue()
    }

    @Test
    fun `un evento en curso no se marca como finalizado`() {
        val enCurso = event(startAt = inicio, endAt = inicio + 7_200_000L)

        assertThat(enCurso.marker(nowMillis = inicio + 3_600_000L).hasFinished).isFalse()
    }

    @Test
    fun `un evento cancelado se marca como cancelado`() {
        assertThat(event(status = EventStatus.CANCELLED).marker().isCancelled).isTrue()
    }

    /**
     * Lo que escribió el organizador dice más que el barrio que resolvió el
     * geocodificador, así que manda sobre él.
     */
    @Test
    fun `el nombre del lugar del organizador manda sobre el del geocodificador`() {
        val conSitio = event(venueName = "Parque de la 93")

        assertThat(conSitio.marker().lugarTexto).isEqualTo("Parque de la 93")
    }

    @Test
    fun `sin nombre de sitio se cae a la direccion y luego al barrio`() {
        assertThat(event(venueName = null, address = "Calle 93 #13-50").marker().lugarTexto)
            .isEqualTo("Calle 93 #13-50")
        assertThat(event(venueName = null, address = null).marker().lugarTexto)
            .isEqualTo("Chapinero, Bogotá")
    }

    /** La copia del organizador puede venir vacía: mismo respaldo que las reseñas. */
    @Test
    fun `sin organizador se usa el nombre de respaldo`() {
        assertThat(event(organizer = null).marker().nombreOrganizador).isEqualTo("Usuario")
    }

    /**
     * Orden estable: si las características bailaran entre emisiones del
     * listener, la tarjeta parpadearía.
     */
    @Test
    fun `las caracteristicas salen en orden estable`() {
        val desordenadas = event(
            features = setOf(EventFeature.FOOD_TRUCKS, EventFeature.EXHIBICION, EventFeature.MUSICA)
        )

        assertThat(desordenadas.marker().features).isEqualTo(
            listOf(EventFeature.EXHIBICION, EventFeature.FOOD_TRUCKS, EventFeature.MUSICA)
        )
    }

    @Test
    fun `los contadores negativos se normalizan a cero`() {
        val corrupto = event(interestedCount = -3, goingCount = -1)

        val marcador = corrupto.marker()

        assertThat(marcador.interestedCount).isEqualTo(0)
        assertThat(marcador.goingCount).isEqualTo(0)
    }

    @Test
    fun `las coordenadas y el tipo viajan tal cual`() {
        val marcador = event(type = EventType.PIQUE).marker()

        assertThat(marcador.lat).isEqualTo(4.6280)
        assertThat(marcador.lng).isEqualTo(-74.0643)
        assertThat(marcador.type).isEqualTo(EventType.PIQUE)
    }

    private fun Event.marker(
        currentUserId: String? = "uid-ana",
        nowMillis: Long = inicio - 1
    ) = toEventMapMarkerUi(
        currentUserId = currentUserId,
        nowMillis = nowMillis,
        fechaTexto = "mañana, 7:00 p. m."
    )

    private fun event(
        type: EventType = EventType.CAR_MEET,
        status: EventStatus = EventStatus.SCHEDULED,
        venueName: String? = "Parque de la 93",
        address: String? = "Calle 93 #13-50",
        startAt: Long = inicio,
        endAt: Long? = null,
        features: Set<EventFeature> = emptySet(),
        organizer: EventOrganizer? = EventOrganizer(id = "uid-ana", fullName = "Ana Organiza"),
        interestedCount: Int = 0,
        goingCount: Int = 0
    ) = Event(
        id = "evt-1",
        type = type,
        title = "Encuentro en el 93",
        description = "Nos vemos temprano",
        organizerId = "uid-ana",
        organizer = organizer,
        location = bogota,
        venueName = venueName,
        address = address,
        startAt = startAt,
        endAt = endAt,
        status = status,
        features = features,
        interestedCount = interestedCount,
        goingCount = goingCount
    )
}
