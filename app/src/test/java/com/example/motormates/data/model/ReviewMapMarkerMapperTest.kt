package com.example.motormates.data.model

import com.example.motormates.data.util.toEpochMillisOrNull
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/**
 * El mapeo dominio -> marcador del mapa. Es una función pura, así que no hace
 * falta MainDispatcherRule ni mocks.
 */
class ReviewMapMarkerMapperTest {

    private val bogota = ReviewLocation(lat = 4.6280, lng = -74.0643, placeName = "Chapinero, Bogotá")

    @Test
    fun `una resenia sin ubicacion no genera marcador`() {
        val sinUbicacion = review(location = null)

        assertThat(sinUbicacion.toReviewMapMarkerUi()).isNull()
    }

    @Test
    fun `una resenia con ubicacion se mapea completa`() {
        val marcador = review(
            location = bogota,
            author = ReviewAuthor(id = "uid-jose", fullName = "Jose Perfil", avatarUrl = "https://foto"),
            vehicle = ReviewVehicleRef(id = "1", brand = "Porsche", model = "911 GT3")
        ).toReviewMapMarkerUi()

        assertThat(marcador).isNotNull()
        assertThat(marcador!!.lat).isEqualTo(4.6280)
        assertThat(marcador.lng).isEqualTo(-74.0643)
        assertThat(marcador.nombreUsuario).isEqualTo("Jose Perfil")
        assertThat(marcador.avatarUrl).isEqualTo("https://foto")
        assertThat(marcador.vehicleName).isEqualTo("Porsche 911 GT3")
        assertThat(marcador.comentario).isEqualTo("Brutal")
        assertThat(marcador.calificacion).isEqualTo(5)
        assertThat(marcador.lugarTexto).isEqualTo("Chapinero, Bogotá")
    }

    /**
     * Una reseña con coordenadas pero sin nombre de sitio sigue generando
     * marcador: lo que falta es el texto, no el lugar.
     */
    @Test
    fun `con ubicacion pero sin nombre el marcador existe sin lugarTexto`() {
        val sinNombre = ReviewLocation(lat = 4.6280, lng = -74.0643, placeName = null)

        val marcador = review(location = sinNombre).toReviewMapMarkerUi()

        assertThat(marcador).isNotNull()
        assertThat(marcador?.lugarTexto).isNull()
    }

    /** Mismos respaldos que el feed: la copia desnormalizada puede faltar. */
    @Test
    fun `sin autor ni vehiculo usa los textos de respaldo`() {
        val marcador = review(location = bogota, author = null, vehicle = null).toReviewMapMarkerUi()

        assertThat(marcador?.nombreUsuario).isEqualTo("Usuario")
        assertThat(marcador?.vehicleName).isEqualTo("Vehículo")
        assertThat(marcador?.avatarUrl).isNull()
    }

    @Test
    fun `una calificacion fuera de rango se recorta a 0 y 5`() {
        assertThat(review(location = bogota, rating = 9).toReviewMapMarkerUi()?.calificacion).isEqualTo(5)
        assertThat(review(location = bogota, rating = -3).toReviewMapMarkerUi()?.calificacion).isEqualTo(0)
    }

    @Test
    fun `sin comentario el texto queda vacio y no nulo`() {
        assertThat(review(location = bogota, comment = null).toReviewMapMarkerUi()?.comentario).isEmpty()
    }

    @Test
    fun `la foto del carro y los likes pasan al marcador`() {
        val marker = review(location = bogota).copy(likesCount = 7)
            .toReviewMapMarkerUi(vehicleImageUrl = "https://porsche.jpg")

        assertThat(marker?.vehicleImageUrl).isEqualTo("https://porsche.jpg")
        assertThat(marker?.likesCount).isEqualTo(7)
    }

    @Test
    fun `una foto en blanco queda null para usar el icono de respaldo`() {
        val marker = review(location = bogota).toReviewMapMarkerUi(vehicleImageUrl = "  ")

        assertThat(marker?.vehicleImageUrl).isNull()
    }

    // ---------- toEpochMillisOrNull ----------
    // Es lo que permite recortar la ventana de 24 h en memoria: el dominio ya
    // perdió los millis y guarda la fecha como texto ISO.

    @Test
    fun `toEpochMillisOrNull devuelve los millis de una fecha ISO valida`() {
        val millis = 1_700_000_000_000L

        assertThat(Instant.ofEpochMilli(millis).toString().toEpochMillisOrNull()).isEqualTo(millis)
    }

    @Test
    fun `toEpochMillisOrNull devuelve null con texto nulo vacio o invalido`() {
        assertThat(null.toEpochMillisOrNull()).isNull()
        assertThat("".toEpochMillisOrNull()).isNull()
        assertThat("   ".toEpochMillisOrNull()).isNull()
        // No debe lanzar excepción: un formato raro solo significa "no sé cuándo".
        assertThat("ayer por la tarde".toEpochMillisOrNull()).isNull()
    }

    private fun review(
        location: ReviewLocation?,
        rating: Int = 5,
        comment: String? = "Brutal",
        author: ReviewAuthor? = null,
        vehicle: ReviewVehicleRef? = null
    ) = Review(
        id = "r1",
        rating = rating,
        comment = comment,
        userId = "uid-jose",
        vehicleId = "1",
        createdAt = Instant.now().toString(),
        author = author,
        vehicle = vehicle,
        location = location
    )
}
