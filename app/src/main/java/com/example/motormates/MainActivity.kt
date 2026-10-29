package com.example.motormates

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.motormates.di.ACTIVE_BACKEND
import com.example.motormates.di.DataBackend
import com.example.motormates.ui.theme.MotorMatesTheme
import dagger.hilt.android.AndroidEntryPoint

// String literal porque la constante Manifest.permission puede no existir en el SDK de compilación
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestLocalNetworkPermissionIfNeeded()
        requestNotificationPermissionIfNeeded()
        setContent {
            MotorMatesTheme {
                MotorMatesApp()
            }
        }
    }

    /**
     * Android 17 bloquea las conexiones a la red local (donde corre el
     * backend propio) si la app no tiene este permiso en tiempo de
     * ejecución. Con Firebase no hace falta, así que solo se pide en modo
     * Express.
     */
    private fun requestLocalNetworkPermissionIfNeeded() {
        if (ACTIVE_BACKEND != DataBackend.EXPRESS || Build.VERSION.SDK_INT < 37) return
        val granted = ContextCompat.checkSelfPermission(this, ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) localNetworkPermissionLauncher.launch(ACCESS_LOCAL_NETWORK)
    }

    /**
     * Desde Android 13 el sistema descarta las notificaciones de una app que
     * no tenga este permiso, así que sin pedirlo el push de la Cloud Function
     * llegaría pero no se vería. Este sí se pide con cualquier backend: FCM
     * siempre va por Firebase.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
