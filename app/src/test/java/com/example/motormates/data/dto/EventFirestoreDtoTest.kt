package com.example.motormates.data.dto

import com.example.motormates.data.model.EventFeature
import com.example.motormates.data.model.EventStatus
import com.example.motormates.data.model.EventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El mapeo documento -> dominio de un evento. Es una función pura, así que no
 * hace falta MainDispatcherRule ni mocks.
 *
 * Lo que de verdad se prueba aquí es que un documento mal formado se
 * descarte en silencio en vez de tumbar el listener del mapa.
 */
class EventFirestoreDtoTest {

    private val bogota = ReviewLocationSnapshot(lat = 4.6280, lng = -74.0643, placeName = "Chapinero")
    private val inicio = 1_700_000_000_000L

    @Test
    fun `un evento completo se mapea entero`() {
        // Arrange
        val dto = dto(
            type = "CAR_MEET",
            features = listOf("EXHIBICION", "FOOD_TRUCKS"),
            user = ReviewUserSnapshot(fullName = "Ana Organiza", avatarUrl = "https://foto")
        )

        // Act
        val evento = dto.toDomain()

        // Assert
        assertThat(evento).isNotNull()
        assertThat(evento!!.type).isEqualTo(EventType.CAR_MEET)
        assertThat(evento.title).isEqualTo("Encuentro en el 93")
        assertThat(evento.location.lat).isEqualTo(4.6280)
        assertThat(evento.organizerId).isEqualTo("uid-ana")
        assertThat(evento.organizer?.fullName).isEqualTo("Ana Organiza")
        assertThat(evento.features)
            .containsExactly(EventFeature.EXHIBICION, EventFeature.FOOD_TRUCKS)
        assertThat(evento.status).isEqualTo(EventStatus.SCHEDULED)
    }

    @Test
    fun `un evento sin ubicacion se descarta`() {
        assertThat(dto(location = null).toDomain()).isNull()
    }

    /**
     * lat/lng nulables en el snapshot: media coordenada no es una coordenada,
     * y un 0.0 por defecto pondría el evento en el golfo de Guinea.
     */
    @Test
    fun `un evento con media coordenada se descarta`() {
        val mediaCoordenada = ReviewLocationSnapshot(lat = 4.6280, lng = null)

        assertThat(dto(location = mediaCoordenada).toDomain()).isNull()
    }

    @Test
    fun `un evento sin titulo se descarta`() {
        assertThat(dto(title = "   ").toDomain()).isNull()
    }

    @Test
    fun `un evento sin fecha de inicio se descarta`() {
        assertThat(dto(startAt = 0L).toDomain()).isNull()
    }

    /**
     * El caso que justifica guardar el tipo como texto y no como enum: con
     * enum, toObject() lanzaría y se caería el snapshot completo.
     */
    @Test
    fun `un tipo desconocido descarta solo ese evento`() {
        assertThat(dto(type = "RODADA_NOCTURNA").toDomain()).isNull()
    }

    @Test
    fun `los atributos desconocidos se ignoran sin perder el evento`() {
        val evento = dto(features = listOf("EXHIBICION", "KARAOKE")).toDomain()

        assertThat(evento).isNotNull()
        assertThat(evento!!.features).containsExactly(EventFeature.EXHIBICION)
    }

    /**
     * Un estado desconocido no descarta el evento: perderlo entero por no
     * entender su estado sería peor que mostrarlo como programado.
     */
    @Test
    fun `un estado desconocido se lee como programado`() {
        val evento = dto(status = "POSPUESTO").toDomain()

        assertThat(evento?.status).isEqualTo(EventStatus.SCHEDULED)
    }

    @Test
    fun `un evento cancelado conserva su estado`() {
        assertThat(dto(status = "CANCELLED").toDomain()?.status).isEqualTo(EventStatus.CANCELLED)
    }

    /**
     * Un fin anterior al inicio es un dato corrupto: se trata como si no se
     * hubiera anunciado hora de fin, en vez de propagar un rango imposible.
     */
    @Test
    fun `un fin anterior al inicio se lee como sin fin`() {
        val evento = dto(endAt = inicio - 3_600_000L).toDomain()

        assertThat(evento?.endAt).isNull()
    }

    @Test
    fun `un fin posterior al inicio se conserva`() {
        val fin = inicio + 3_600_000L

        assertThat(dto(endAt = fin).toDomain()?.endAt).isEqualTo(fin)
    }

    /** Sin captura del organizador el evento existe; lo que falta es el nombre. */
    @Test
    fun `sin captura del organizador el evento existe sin organizer`() {
        val evento = dto(user = null).toDomain()

        assertThat(evento).isNotNull()
        assertThat(evento!!.organizer).isNull()
        assertThat(evento.organizerId).isEqualTo("uid-ana")
    }

    @Test
    fun `los textos en blanco se normalizan a null`() {
        val evento = dto(description = "  ", venueName = "", address = " ").toDomain()

        assertThat(evento?.description).isNull()
        assertThat(evento?.venueName).isNull()
        assertThat(evento?.address).isNull()
    }

    private fun dto(
        type: String = "PIQUE",
        title: String = "Encuentro en el 93",
        description: String? = "Nos vemos temprano",
        location: ReviewLocationSnapshot? = bogota,
        venueName: String? = "Parque de la 93",
        address: String? = "Calle 93 #13-50",
        startAt: Long = inicio,
        endAt: Long? = null,
        status: String = "SCHEDULED",
        features: List<String> = emptyList(),
        user: ReviewUserSnapshot? = ReviewUserSnapshot(fullName = "Ana Organiza")
    ) = EventFirestoreDto(
        id = "evt-1",
        type = type,
        title = title,
        description = description,
        userId = "uid-ana",
        user = user,
        location = location,
        venueName = venueName,
        address = address,
        startAt = startAt,
        endAt = endAt,
        status = status,
        features = features
    )
}
