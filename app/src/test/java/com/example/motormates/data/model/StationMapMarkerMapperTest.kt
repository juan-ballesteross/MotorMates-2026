package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * El mapper de estación a marcador.
 *
 * Lo único que decide es el respaldo de los textos, y ahí está justamente lo
 * que la pantalla no debe tener que repetir: la tarjeta recibe un nombre
 * siempre presente, y una dirección que puede faltar para poder omitir la
 * línea en vez de dejarla vacía.
 */
class StationMapMarkerMapperTest {

    @Test
    fun `copia los datos y el tipo que se pidio`() {
        val ui = station(name = "Terpel 85", address = "Calle 85 #15-20")
            .toStationMapMarkerUi(StationKind.FUEL)

        assertThat(ui.id).isEqualTo("p1")
        assertThat(ui.kind).isEqualTo(StationKind.FUEL)
        assertThat(ui.nombre).isEqualTo("Terpel 85")
        assertThat(ui.direccion).isEqualTo("Calle 85 #15-20")
        assertThat(ui.lat).isEqualTo(4.67)
        assertThat(ui.lng).isEqualTo(-74.05)
    }

    @Test
    fun `sin nombre pone el respaldo del tipo`() {
        assertThat(station(name = null).toStationMapMarkerUi(StationKind.FUEL).nombre)
            .isEqualTo("Gasolinera")

        assertThat(station(name = null).toStationMapMarkerUi(StationKind.CHARGING).nombre)
            .isEqualTo("Estación de carga")
    }

    /** Un nombre en blanco cuenta como ausente: no se pinta una tarjeta muda. */
    @Test
    fun `un nombre en blanco tambien usa el respaldo`() {
        assertThat(station(name = "   ").toStationMapMarkerUi(StationKind.FUEL).nombre)
            .isEqualTo("Gasolinera")
    }

    /**
     * La dirección se queda en null y NO cae a un texto de relleno: la tarjeta
     * usa el null para omitir la línea entera.
     */
    @Test
    fun `sin direccion queda en null`() {
        assertThat(station(address = null).toStationMapMarkerUi(StationKind.FUEL).direccion)
            .isNull()

        assertThat(station(address = "  ").toStationMapMarkerUi(StationKind.FUEL).direccion)
            .isNull()
    }

    private fun station(
        name: String? = "Terpel",
        address: String? = "Calle 85"
    ) = Station(
        id = "p1",
        name = name,
        address = address,
        location = ReviewLocation(lat = 4.67, lng = -74.05)
    )
}
