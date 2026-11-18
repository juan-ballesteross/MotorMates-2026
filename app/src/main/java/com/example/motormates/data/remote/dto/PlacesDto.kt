package com.example.motormates.data.remote.dto

import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.model.Station

/**
 * El cuerpo y la respuesta de Places API (New), `places:searchNearby`.
 *
 * Los nombres de las propiedades se dejan igual que los del JSON (camelCase,
 * como los manda Google) para no tener que anotar cada campo con
 * @SerializedName: Gson los empareja directo.
 *
 * Solo están los campos que de verdad se piden. El `X-Goog-FieldMask` de la
 * petición decide qué devuelve Google Y a qué tarifa cobra, así que agregar
 * un campo acá sin agregarlo al field mask no sirve de nada, y agregarlo al
 * field mask sin necesitarlo encarece TODAS las consultas de la capa.
 */
data class SearchNearbyRequest(
    val includedTypes: List<String>,
    /** Entre 1 y 20: es el tope que impone la API. */
    val maxResultCount: Int,
    /** DISTANCE o POPULARITY. */
    val rankPreference: String,
    val locationRestriction: LocationRestrictionDto,
    /** Para que los nombres y las direcciones vengan en español. */
    val languageCode: String
)

data class LocationRestrictionDto(val circle: CircleDto)

/** radius va en metros, tiene que ser > 0 y no puede pasar de 50 000. */
data class CircleDto(val center: LatLngDto, val radius: Double)

data class LatLngDto(val latitude: Double, val longitude: Double)

/**
 * `places` es nulable a propósito: cuando no hay ningún resultado en el
 * círculo, Places no devuelve una lista vacía sino un objeto vacío `{}`.
 */
data class SearchNearbyResponse(val places: List<PlaceDto>? = null)

data class PlaceDto(
    val id: String? = null,
    val displayName: LocalizedTextDto? = null,
    val formattedAddress: String? = null,
    val location: LatLngDto? = null
)

/** Google envuelve los textos traducibles en un objeto con su idioma. */
data class LocalizedTextDto(val text: String? = null, val languageCode: String? = null)

/**
 * Null si al lugar le falta lo imprescindible, igual que
 * EventFirestoreDto.toDomain(): sin id no se puede seleccionar y sin
 * coordenadas no se puede pintar, así que no hay marcador que valga. El
 * repositorio los descarta con mapNotNull en vez de propagar el error.
 *
 * El nombre y la dirección sí pueden faltar: la tarjeta tiene respaldo.
 */
fun PlaceDto.toDomain(): Station? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val position = location ?: return null

    return Station(
        id = id,
        name = displayName?.text?.takeIf { it.isNotBlank() },
        address = formattedAddress?.takeIf { it.isNotBlank() },
        location = ReviewLocation(lat = position.latitude, lng = position.longitude)
    )
}
