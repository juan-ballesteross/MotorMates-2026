package com.example.motormates.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            runCatching { lastLocation() ?: currentLocation() }.getOrNull()
        }
    }

    /**
     * La caché del sistema: responde al instante, pero viene null en un
     * emulador recién abierto o en un teléfono que no ha usado el GPS en un
     * rato, que es el caso más común y no la excepción.
     */
    private suspend fun lastLocation(): ReviewLocation? =
        locationClient.lastLocation.await()?.let { ReviewLocation(it.latitude, it.longitude) }

    /**
     * Si la caché está vacía se pide una posición de verdad. BALANCED y no
     * HIGH_ACCURACY: al mapa le basta la precisión del barrio y así no se
     * prende el GPS fino para guardar un alfiler.
     */
    private suspend fun currentLocation(): ReviewLocation? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()
        return locationClient.getCurrentLocation(request, null).await()
            ?.let { ReviewLocation(it.latitude, it.longitude) }
    }

    private fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        /**
         * Tope para que publicar una reseña nunca se quede esperando un fix
         * que no va a llegar. Si se vence, la reseña sale sin ubicación.
         */
        const val LOCATION_TIMEOUT_MS = 5_000L
    }
}
