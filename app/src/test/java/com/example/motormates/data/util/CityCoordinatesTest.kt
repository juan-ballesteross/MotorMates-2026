package com.example.motormates.data.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CityCoordinatesTest {

    @Test
    fun `encuentra la ciudad sin importar tildes ni mayusculas`() {
        assertThat(CityCoordinates.find("bogota")?.name).isEqualTo("Bogotá")
        assertThat(CityCoordinates.find(" MEDELLÍN ")?.name).isEqualTo("Medellín")
        assertThat(CityCoordinates.find("Tunja")).isNull()
        assertThat(CityCoordinates.find(null)).isNull()
    }

    @Test
    fun `saca la ciudad de la ubicacion libre del perfil`() {
        assertThat(CityCoordinates.findIn("Medellín, Colombia")?.name).isEqualTo("Medellín")
        assertThat(CityCoordinates.findIn("Vivo en cali")?.name).isEqualTo("Cali")
        assertThat(CityCoordinates.findIn("")).isNull()
    }

    @Test
    fun `la ciudad mas cercana dentro del radio`() {
        // Chía queda a ~25 km del centro de Bogotá.
        assertThat(CityCoordinates.nearest(4.8616, -74.0329)?.name).isEqualTo("Bogotá")
        // Villavicencio (~75 km): fuera de todas.
        assertThat(CityCoordinates.nearest(4.1420, -73.6266)).isNull()
    }

    @Test
    fun `distancia aproximada entre Bogota y Medellin`() {
        val bogota = CityCoordinates.find("Bogotá")!!
        val medellin = CityCoordinates.find("Medellín")!!

        val km = CityCoordinates.distanceKm(bogota.lat, bogota.lng, medellin.lat, medellin.lng)

        assertThat(km).isWithin(15.0).of(240.0)
    }
}
