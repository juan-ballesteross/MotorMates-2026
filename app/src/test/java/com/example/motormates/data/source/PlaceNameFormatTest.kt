package com.example.motormates.data.source

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El armado del texto del lugar. Se prueba aparte del Geocoder porque ese
 * necesita dispositivo y red; esta parte es una función pura y es donde de
 * verdad están las decisiones.
 */
class PlaceNameFormatTest {

    @Test
    fun `con barrio y ciudad se escriben los dos`() {
        val texto = formatPlaceName(neighborhood = "Chapinero", city = "Bogotá", region = "Cundinamarca")

        assertThat(texto).isEqualTo("Chapinero, Bogotá")
    }

    @Test
    fun `sin barrio queda la ciudad`() {
        assertThat(formatPlaceName(neighborhood = null, city = "Bogotá", region = "Cundinamarca"))
            .isEqualTo("Bogotá")
    }

    @Test
    fun `sin barrio ni ciudad queda la region`() {
        assertThat(formatPlaceName(neighborhood = null, city = null, region = "Cundinamarca"))
            .isEqualTo("Cundinamarca")
    }

    @Test
    fun `sin ningun dato devuelve null`() {
        assertThat(formatPlaceName(neighborhood = null, city = null, region = null)).isNull()
    }

    /** Los campos vacíos o con espacios cuentan como ausentes, no como texto. */
    @Test
    fun `los campos en blanco se tratan como ausentes`() {
        assertThat(formatPlaceName(neighborhood = "  ", city = "Bogotá", region = null))
            .isEqualTo("Bogotá")
        assertThat(formatPlaceName(neighborhood = "", city = "", region = "  ")).isNull()
    }

    @Test
    fun `se recortan los espacios de sobra`() {
        assertThat(formatPlaceName(neighborhood = "  Chapinero ", city = " Bogotá  ", region = null))
            .isEqualTo("Chapinero, Bogotá")
    }

    /**
     * Algunos sitios devuelven el barrio con el mismo nombre de la ciudad;
     * escribirlo dos veces ("Bogotá, Bogotá") se ve a error.
     */
    @Test
    fun `si el barrio y la ciudad son iguales no se repite el nombre`() {
        assertThat(formatPlaceName(neighborhood = "Bogotá", city = "Bogotá", region = null))
            .isEqualTo("Bogotá")
        // La comparación ignora mayúsculas: igual se considera repetido.
        assertThat(formatPlaceName(neighborhood = "bogotá", city = "Bogotá", region = null))
            .isEqualTo("bogotá")
    }
}
