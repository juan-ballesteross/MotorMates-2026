package com.example.motormates.e2e

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Runner de las pruebas instrumentadas.
 *
 * Las pruebas E2E manejan la app de verdad, así que la app tiene que hablar
 * con el Firebase Emulator Suite y no con el proyecto real. No se hace
 * cambiando BuildConfig.CONNECT_TO_FIREBASE_EMULATOR (que está en false a
 * propósito, para que las ejecuciones manuales en la tablet sigan yendo a
 * producción) sino aquí: callApplicationOnCreate corre ANTES del onCreate de
 * MotorMatesApplication, y useEmulator() solo vale si se llama antes de la
 * primera lectura o escritura.
 *
 * Las instancias que se configuran son las de POR DEFECTO, que son las
 * mismas que después inyecta Hilt (AuthFirebaseModule y FirestoreModule).
 * No se pisan las de integration.FirebaseEmulator, que usa un FirebaseApp
 * aparte llamado "integration-test".
 */
class EmulatorTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        // MainActivity pide estos dos permisos al arrancar y el diálogo del
        // sistema taparía los clics de la prueba. Se conceden antes de que
        // exista la Activity, igual que hace integration.FirebaseEmulator.
        if (Build.VERSION.SDK_INT >= 33) {
            grant("android.permission.POST_NOTIFICATIONS")
        }
        if (Build.VERSION.SDK_INT >= 37) {
            // Desde Android 17 llegar a 10.0.2.2 (la red local) es un permiso
            // en tiempo de ejecución: sin él las conexiones se quedan colgadas.
            grant("android.permission.ACCESS_LOCAL_NETWORK")
        }
        super.onCreate(arguments)
    }

    override fun callApplicationOnCreate(app: Application) {
        FirebaseApp.initializeApp(app)
        // useEmulator lanza IllegalStateException si ya se llamó: pasa al
        // correr varias clases de prueba en el mismo proceso.
        runCatching { FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT) }
        runCatching { FirebaseFirestore.getInstance(DATABASE_ID).useEmulator(HOST, FIRESTORE_PORT) }
        super.callApplicationOnCreate(app)
    }

    private fun grant(permission: String) = runCatching {
        uiAutomation.grantRuntimePermission(targetContext.packageName, permission)
    }

    private companion object {
        const val HOST = "10.0.2.2"
        const val AUTH_PORT = 9099
        const val FIRESTORE_PORT = 8080

        // La app no usa la base "(default)" sino esta (ver FirestoreModule).
        const val DATABASE_ID = "motormatesdb"
    }
}
