package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Agrupar en el mapa las reseñas que están a 30 m o menos. */
class MapGroupingTest {

    /** ~1 m de latitud en grados (cerca del ecuador, como Bogotá). */
    private val oneMeterLat = 1.0 / 111_320.0

    private val base = 4.6280 to -74.0643

    private fun marker(id: String, northMeters: Double = 0.0) = ReviewMapMarkerUi(
        id = id, userId = "u", vehicleId = "v", lat = base.first + northMeters * oneMeterLat, lng = base.second,
        nombreUsuario = "Ana", vehicleName = "Mazda MX-5", tiempoTexto = "hace 1 h", calificacion = 5, comentario = ""
    )

    @Test
    fun `la distancia entre dos puntos sale en metros`() {
        val d = distanceMeters(base.first, base.second, base.first + 20 * oneMeterLat, base.second)

        assertThat(d).isWithin(0.5).of(20.0)
    }

    @Test
    fun `de Bogota a Medellin son unos 240 km`() {
        val d = distanceMeters(4.6280, -74.0643, 6.2442, -75.5812)

        assertThat(d / 1000).isWithin(10.0).of(245.0)
    }

    @Test
    fun `resenas a menos de 30 metros quedan en un solo grupo`() {
        val groups = groupNearby(listOf(marker("a"), marker("b", 12.0), marker("c", 25.0)))

        assertThat(groups.map { g -> g.map { it.id } }).containsExactly(listOf("a", "b", "c"))
    }

    @Test
    fun `a mas de 30 metros quedan separadas`() {
        val groups = groupNearby(listOf(marker("a"), marker("b", 45.0)))

        assertThat(groups.map { g -> g.map { it.id } }).containsExactly(listOf("a"), listOf("b"))
    }

    @Test
    fun `justo en el limite de 30 metros todavia se juntan`() {
        assertThat(listOf(marker("a"), marker("b", 29.9)).areNearbyGroup()).isTrue()
        assertThat(listOf(marker("a"), marker("b", 31.0)).areNearbyGroup()).isFalse()
    }

    @Test
    fun `cada resena queda en un solo grupo y no se pierde ninguna`() {
        val markers = listOf(marker("a"), marker("b", 10.0), marker("c", 100.0), marker("d", 110.0), marker("e", 500.0))

        val groups = groupNearby(markers)

        assertThat(groups.flatten().map { it.id }).containsExactly("a", "b", "c", "d", "e")
        assertThat(groups.map { g -> g.map { it.id } })
            .containsExactly(listOf("a", "b"), listOf("c", "d"), listOf("e")).inOrder()
    }

    @Test
    fun `sin resenas no hay grupos`() {
        assertThat(groupNearby(emptyList())).isEmpty()
        assertThat(emptyList<ReviewMapMarkerUi>().areNearbyGroup()).isFalse()
    }
}
