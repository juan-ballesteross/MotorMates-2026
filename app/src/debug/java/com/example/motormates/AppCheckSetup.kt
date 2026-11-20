package com.example.motormates

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

/**
 * Debug: proveedor de depuración de App Check. Al arrancar, imprime en
 * logcat un "debug token" (busca "DebugAppCheckProvider"); hay que
 * registrarlo en la consola de Firebase > App Check > Administrar tokens de
 * depuración antes de activar la aplicación forzosa (enforcement).
 *
 * Hay una versión de este archivo por tipo de build (src/debug y
 * src/release) porque la librería de depuración solo se incluye en debug.
 */
object AppCheckSetup {
    fun install(application: Application) {
        Firebase.initialize(application)
        Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
