package com.example.motormates.data.model

/**
 * Una estación: hoy solo gasolineras, mañana también de carga eléctrica.
 *
 * No vive en Firestore, al contrario que [Review] y [Event]: es un lugar que
 * devuelve Google Places y que la app ni guarda ni edita. Por eso no tiene
 * autor, ni fechas, ni contadores, y por eso tampoco hay reglas de Firestore
 * ni DTO de Firestore que lo acompañen.
 *
 * Se reutiliza [ReviewLocation] tal cual, igual que lo hace [Event]: se llama
 * así por dónde nació, pero ya es el tipo de ubicación genérico de la app.
 *
 * [name] y [address] son nulables porque el field mask barato no garantiza
 * ninguno de los dos para todos los lugares; el mapper a la UI pone el
 * respaldo. [id] y la ubicación, en cambio, son obligatorios: sin id no se
 * puede seleccionar y sin coordenadas no se puede pintar.
 */
data class Station(
    /** El place_id de Google. Sirve además para abrirla en otra app de mapas. */
    val id: String,
    val name: String?,
    val address: String?,
    val location: ReviewLocation
)

/**
 * Qué clase de estación se pide a Places.
 *
 * El valor es el tipo tal como lo nombra Google en `includedTypes`. Es un
 * enum y no un String suelto para que agregar la capa de carga eléctrica sea
 * una constante más, y no otra integración: el resto del camino (fuente,
 * repositorio, ViewModel) ya va parametrizado por esto.
 */
enum class StationKind(val placesType: String) {
    FUEL("gas_station"),
    CHARGING("electric_vehicle_charging_station")
}
