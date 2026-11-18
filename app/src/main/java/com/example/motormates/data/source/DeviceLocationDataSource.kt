package com.example.motormates.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import com.example.motormates.data.model.ReviewLocation
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * De dónde sale la ubicación que se guarda en una reseña.
 *
 * No pasa por el pick() de DataSourceModule como las demás fuentes: esto no
 * es un backend, es una capacidad del teléfono, y es la misma con Firestore o
 * con el backend propio. Se declara como interfaz igual para que el ViewModel
 * se pueda probar con MockK sin tocar Google Play Services.
 */
interface DeviceLocationDataSource {

    /**
     * La ubicación actual, o null si no se pudo obtener: sin permiso, sin
     * GPS, sin señal o con Play Services caído. Nunca lanza una excepción,
     * porque una reseña se tiene que poder publicar aunque no haya ubicación.
     */
    suspend fun lastKnownLocation(): ReviewLocation?
}

class FusedDeviceLocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationClient: FusedLocationProviderClient
) : DeviceLocationDataSource {

    override suspend fun lastKnownLocation(): ReviewLocation? {
        // Se revisa el permiso en vez de confiar en que la pantalla ya lo
        // pidió: así esta clase es segura desde cualquier llamador.
        if (!hasLocationPermission()) return null

        // Primero una posición fresca. Antes se usaba primero la caché del
        // sistema, que puede ser de hace horas o venir de antenas (km de error):
        // por eso las reseñas aparecían lejos de donde se publicaron.
        val fresh = withTimeoutOrNull(FRESH_LOCATION_TIMEOUT_MS) {
            runCatching { currentLocation() }.getOrNull()
        }
        if (fresh != null) return fresh

        // Sin señal a tiempo: la caché sirve solo si es reciente.
        return withTimeoutOrNull(CACHED_LOCATION_TIMEOUT_MS) {
            runCatching { recentCachedLocation() }.getOrNull()
        }
    }

    /**
     * Posición actual. Con permiso preciso usa el GPS (HIGH_ACCURACY); con
     * solo el aproximado, Android igual la movería hasta ~2 km a propósito,
     * así que no vale la pena prender el GPS. setMaxUpdateAgeMillis deja
     * reutilizar una posición de hace pocos segundos sin esperar un fix nuevo.
     */
    private suspend fun currentLocation(): ReviewLocation? {
        val priority = if (hasPreciseLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(MAX_FRESH_AGE_MS)
            .setDurationMillis(FRESH_LOCATION_TIMEOUT_MS)
            .build()
        return locationClient.getCurrentLocation(request, null).await()
            ?.let { ReviewLocation(it.latitude, it.longitude) }
    }

    /** La caché del sistema, descartada si tiene más de un par de minutos. */
    private suspend fun recentCachedLocation(): ReviewLocation? {
        val location = locationClient.lastLocation.await() ?: return null
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        return if (ageMs <= MAX_CACHED_AGE_MS) ReviewLocation(location.latitude, location.longitude) else null
    }

    private fun hasPreciseLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        /**
         * Tope para el fix fresco. Corre al abrir el formulario (no al
         * publicar), así que unos segundos de espera no bloquean nada.
         */
        const val FRESH_LOCATION_TIMEOUT_MS = 8_000L

        /** La caché responde casi al instante; esto es solo un seguro. */
        const val CACHED_LOCATION_TIMEOUT_MS = 2_000L

        /** Una posición de hace menos de esto cuenta como "fresca". */
        const val MAX_FRESH_AGE_MS = 30_000L

        /** Más vieja que esto, la caché ya no se usa. */
        const val MAX_CACHED_AGE_MS = 2 * 60_000L
    }
}
