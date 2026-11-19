package com.example.motormates.data.source

import com.example.motormates.data.model.StationKind
import com.example.motormates.data.remote.dto.CircleDto
import com.example.motormates.data.remote.dto.LatLngDto
import com.example.motormates.data.remote.dto.LocationRestrictionDto
import com.example.motormates.data.remote.dto.PlaceDto
import com.example.motormates.data.remote.dto.SearchNearbyRequest
import com.example.motormates.data.remote.service.PlacesApiService
import javax.inject.Inject
import javax.inject.Named

/** El nombre con el que Hilt provee la llave de Places (ver NetworkModule). */
const val PLACES_API_KEY_NAME = "placesApiKey"

/**
 * Las estaciones de una zona.
 *
 * Devuelve DTOs y no modelos de dominio, igual que [EventDataSource]: mapear
 * y descartar lo que viene incompleto es trabajo del repositorio.
 */
interface StationDataSource {

    /**
     * Si hay llave configurada. Sin ella no se puede consultar nada, y el
     * mapa prefiere no ofrecer el filtro antes que ofrecer uno que falla.
     */
    fun hasApiKey(): Boolean

    /**
     * Las estaciones del [kind] pedido dentro del círculo dado. Lanza lo que
     * lance Retrofit; el repositorio lo traduce.
     */
    suspend fun stationsNear(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        kind: StationKind
    ): List<PlaceDto>
}

class PlacesStationDataSource @Inject constructor(
    private val service: PlacesApiService,
    // @param: y no @Named suelto: sobre un val de constructor, Kotlin está
    // cambiando a aplicar la anotación también a la propiedad, y Dagger la
    // lee del parámetro. Ser explícito evita el aviso del compilador.
    @param:Named(PLACES_API_KEY_NAME) private val apiKey: String
) : StationDataSource {

    override fun hasApiKey(): Boolean = apiKey.isNotBlank()

    override suspend fun stationsNear(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        kind: StationKind
    ): List<PlaceDto> {
        val response = service.searchNearby(
            apiKey = apiKey,
            fieldMask = FIELD_MASK,
            request = SearchNearbyRequest(
                includedTypes = listOf(kind.placesType),
                maxResultCount = MAX_RESULTS,
                rankPreference = RANK_BY_DISTANCE,
                locationRestriction = LocationRestrictionDto(
                    circle = CircleDto(
                        center = LatLngDto(latitude = lat, longitude = lng),
                        // La API rechaza 0 y no acepta más de 50 km.
                        radius = radiusMeters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
                    )
                ),
                languageCode = LANGUAGE_CODE
            )
        )
        return response.places.orEmpty()
    }

    private companion object {
        /**
         * Los campos que se piden, y por lo tanto la tarifa que cobra Google.
         *
         * Están TODOS en la tarifa Pro a propósito. Agregar aquí un campo de
         * tarifa Enterprise (por ejemplo currentOpeningHours, para saber si
         * está abierta) encarecería cada consulta de la capa, no solo las
         * estaciones que el usuario llegue a abrir.
         *
         * El field mask es obligatorio: sin él la API responde 400.
         */
        const val FIELD_MASK =
            "places.id,places.displayName,places.location,places.formattedAddress"

        /** Es el tope que impone la API, no una elección. */
        const val MAX_RESULTS = 20

        /**
         * Las más cercanas al centro de lo que se está mirando. Con el tope de
         * 20 resultados, POPULARITY devolvería 20 estaciones repartidas por
         * todo el círculo y el mapa se vería con huecos alrededor del centro.
         */
        const val RANK_BY_DISTANCE = "DISTANCE"

        const val LANGUAGE_CODE = "es"
        const val MIN_RADIUS_METERS = 1.0
        const val MAX_RADIUS_METERS = 50_000.0
    }
}
