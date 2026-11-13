package com.example.motormates.ui.reviewsMap.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.motormates.R
import com.example.motormates.ui.reviewsMap.LocationAccess
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/**
 * Todo lo que el mapa necesita saber de Android para "Mi ubicación": el
 * permiso, la ubicación del teléfono y Play Services. Vive en la UI porque
 * necesita la Activity (diálogos del sistema, Ajustes); el ViewModel solo
 * recibe el LocationAccess resultante.
 *
 * Casos que cubre:
 * - Ya concedido -> GRANTED.
 * - Nunca pedido -> se explica para qué antes de mostrar el diálogo del sistema.
 * - Negado una vez -> se vuelve a explicar y a pedir.
 * - "No volver a preguntar" -> se ofrece abrir los Ajustes de la app.
 * - Permiso sí, ubicación apagada -> diálogo del sistema para encenderla.
 * - Lo quitó desde Ajustes con la app abierta -> se revisa en ON_RESUME.
 * - Sin Play Services -> UNAVAILABLE y el botón se oculta.
 *
 * Solo pide ACCESS_COARSE_LOCATION: al mapa le basta el barrio, y es el mismo
 * permiso con el que la app adjunta la ubicación al publicar.
 */
class LocationPermissionHandler internal constructor(
    private val requestAction: () -> Unit,
    internal val dialog: MutableState<PermissionDialog?>,
    internal val onConfirmRationale: () -> Unit,
    internal val onOpenSettings: () -> Unit
) {
    /** Lo llama el botón "Mi ubicación". Resuelve lo que falte y, si todo está, avisa con onReady. */
    fun request() = requestAction()
}

internal enum class PermissionDialog { RATIONALE, OPEN_SETTINGS }

@Composable
fun rememberLocationPermissionHandler(
    onAccessChanged: (LocationAccess) -> Unit,
    onReady: () -> Unit
): LocationPermissionHandler {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnAccessChanged by rememberUpdatedState(onAccessChanged)
    val currentOnReady by rememberUpdatedState(onReady)

    // Sin esto no se distingue "nunca se pidió" de "no volver a preguntar":
    // en los dos casos shouldShowRequestPermissionRationale devuelve false.
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    val dialog = remember { mutableStateOf<PermissionDialog?>(null) }

    fun evaluate(): LocationAccess {
        val access = evaluateAccess(context, activity, askedBefore)
        currentOnAccessChanged(access)
        return access
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (evaluate() == LocationAccess.GRANTED && result.resultCode == Activity.RESULT_OK) currentOnReady()
    }

    fun turnOnLocationServices() {
        val activityRef = activity ?: return
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, LOCATION_INTERVAL_MS).build()
            )
            .build()
        LocationServices.getSettingsClient(activityRef)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener { if (evaluate() == LocationAccess.GRANTED) currentOnReady() }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException) {
                    settingsLauncher.launch(IntentSenderRequest.Builder(error.resolution).build())
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        askedBefore = true
        when (evaluate()) {
            LocationAccess.GRANTED -> currentOnReady()
            LocationAccess.SERVICES_OFF -> turnOnLocationServices()
            else -> Unit
        }
    }

    // Se revisa al volver de Ajustes o de otra app: el permiso o la
    // ubicación pudieron cambiar mientras tanto. Así isMyLocationEnabled
    // nunca queda en true sin permiso (lanzaría SecurityException).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { evaluate() }

    return remember(activity) {
        LocationPermissionHandler(
            requestAction = {
                when (evaluate()) {
                    LocationAccess.GRANTED -> currentOnReady()
                    LocationAccess.SERVICES_OFF -> turnOnLocationServices()
                    LocationAccess.NOT_REQUESTED,
                    LocationAccess.DENIED_CAN_ASK -> dialog.value = PermissionDialog.RATIONALE
                    LocationAccess.DENIED_PERMANENTLY -> dialog.value = PermissionDialog.OPEN_SETTINGS
                    LocationAccess.UNAVAILABLE -> Unit
                }
            },
            dialog = dialog,
            onConfirmRationale = {
                dialog.value = null
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onOpenSettings = {
                dialog.value = null
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }
}

/**
 * Los diálogos propios que acompañan al permiso. Se explica para qué antes
 * de mostrar el del sistema: un permiso pedido sin contexto se niega más, y
 * cada negación acerca al "no volver a preguntar".
 */
@Composable
fun LocationPermissionDialogs(handler: LocationPermissionHandler) {
    when (handler.dialog.value) {
        PermissionDialog.RATIONALE -> AlertDialog(
            onDismissRequest = { handler.dialog.value = null },
            title = { Text(stringResource(R.string.reviews_map_location_rationale_title)) },
            text = { Text(stringResource(R.string.reviews_map_location_rationale_body)) },
            confirmButton = {
                TextButton(onClick = handler.onConfirmRationale) {
                    Text(stringResource(R.string.reviews_map_location_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { handler.dialog.value = null }) {
                    Text(stringResource(R.string.reviews_map_location_not_now))
                }
            },
            modifier = androidx.compose.ui.Modifier.testTag("reviewsMapLocationRationale")
        )

        PermissionDialog.OPEN_SETTINGS -> AlertDialog(
            onDismissRequest = { handler.dialog.value = null },
            title = { Text(stringResource(R.string.reviews_map_location_settings_title)) },
            text = { Text(stringResource(R.string.reviews_map_location_settings_body)) },
            confirmButton = {
                TextButton(onClick = handler.onOpenSettings) {
                    Text(stringResource(R.string.reviews_map_location_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { handler.dialog.value = null }) {
                    Text(stringResource(R.string.reviews_map_location_not_now))
                }
            },
            modifier = androidx.compose.ui.Modifier.testTag("reviewsMapLocationSettings")
        )

        null -> Unit
    }
}

private fun evaluateAccess(context: Context, activity: Activity?, askedBefore: Boolean): LocationAccess {
    val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (playServices != ConnectionResult.SUCCESS) return LocationAccess.UNAVAILABLE

    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (granted) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (LocationManagerCompat.isLocationEnabled(locationManager)) {
            LocationAccess.GRANTED
        } else {
            LocationAccess.SERVICES_OFF
        }
    }

    val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true
    return when {
        canAskAgain -> LocationAccess.DENIED_CAN_ASK
        askedBefore -> LocationAccess.DENIED_PERMANENTLY
        else -> LocationAccess.NOT_REQUESTED
    }
}

private const val LOCATION_INTERVAL_MS = 10_000L
