package com.example.motormates

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

/**
 * Release: App Check con Play Integrity, para que solo la app real (no un
 * script con la API key del google-services.json) pueda gastar la cuota de
 * Gemini. Ver la versión de src/debug para el flujo de desarrollo.
 */
object AppCheckSetup {
    fun install(application: Application) {
        Firebase.initialize(application)
        Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
