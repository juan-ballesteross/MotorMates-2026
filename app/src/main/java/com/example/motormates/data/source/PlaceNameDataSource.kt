package com.example.motormates.data.source

import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.motormates.data.model.ReviewLocation
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Convierte unas coordenadas en un nombre que una persona pueda leer
 * ("Chapinero, Bogotá").
 *
 * Se usa una sola vez, al publicar la reseña, y el resultado se guarda dentro
 * del documento. Hacerlo al leer significaría una llamada de red por cada
 * reseña en pantalla.
 */
interface PlaceNameDataSource {

    /**
     * El nombre del sitio, o null si no se pudo resolver (sin red, sin
     * servicio de geocodificación, o una zona sin datos). Nunca lanza: no
     * tener el nombre no debe impedir publicar.
     */
    suspend fun placeNameFor(location: ReviewLocation): String?
}

/**
 * Usa el Geocoder de la plataforma. No necesita API key ni facturación: es
 * parte de Android y se apoya en los servicios de Google del dispositivo.
 * Por eso no se usó la Geocoding API de Google, que sí cobra y exigiría
 * administrar otra llave.
 */
class GeocoderPlaceNameDataSource @Inject constructor(
    private val geocoder: Geocoder
) : PlaceNameDataSource {

    override suspend fun placeNameFor(location: ReviewLocation): String? {
        // En un dispositivo sin servicio de geocodificación (algunas imágenes
        // de emulador sin Google) no hay nada que intentar.
        if (!Geocoder.isPresent()) return null

        return withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            runCatching { firstAddress(location)?.toPlaceName() }.getOrNull()
        }
    }

    private suspend fun firstAddress(location: ReviewLocation): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addressAsync(location)
        } else {
            addressBlocking(location)
        }

    /** Desde API 33 el Geocoder trae una versión con callback, sin bloquear. */
    private suspend fun addressAsync(location: ReviewLocation): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                location.lat,
                location.lng,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses.firstOrNull())
                    }

                    // Un error acá no es excepcional: pasa sin red o fuera de
                    // cobertura del servicio. Se resuelve con null.
                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                }
            )
        }

    /**
     * En API 26..32 solo existe la versión que bloquea (y que está deprecada
     * desde 33). Va en Dispatchers.IO porque hace una llamada de red.
     */
    @Suppress("DEPRECATION")
    private suspend fun addressBlocking(location: ReviewLocation): Address? =
        withContext(Dispatchers.IO) {
            try {
                geocoder.getFromLocation(location.lat, location.lng, 1)?.firstOrNull()
            } catch (e: IOException) {
                null
            }
        }

    private companion object {
        /** Si el servicio no responde, la reseña se publica sin nombre. */
        const val GEOCODER_TIMEOUT_MS = 5_000L
    }
}

private fun Address.toPlaceName(): String? =
    formatPlaceName(
        neighborhood = subLocality,
        city = locality,
        region = adminArea
    )

/**
 * Arma el texto a partir de lo que trajo el geocodificador. Está aparte y es
 * pura para poder probarla sin el Geocoder de la plataforma, que necesita
 * dispositivo y red.
 *
 * Se prefiere "barrio, ciudad" porque es lo que ubica a una persona; si falta
 * el barrio queda la ciudad, y si falta la ciudad, la región. Si no hay nada
 * devuelve null y la reseña simplemente no muestra lugar.
 */
internal fun formatPlaceName(
    neighborhood: String?,
    city: String?,
    region: String?
): String? {
    val barrio = neighborhood?.trim()?.ifBlank { null }
    val ciudad = city?.trim()?.ifBlank { null }
    val region = region?.trim()?.ifBlank { null }

    return when {
        // Cuando el geocodificador repite el nombre (algunos sitios devuelven
        // el barrio igual que la ciudad) no se escribe dos veces.
        barrio != null && ciudad != null && !barrio.equals(ciudad, ignoreCase = true) ->
            "$barrio, $ciudad"
        barrio != null -> barrio
        ciudad != null -> ciudad
        else -> region
    }
}
