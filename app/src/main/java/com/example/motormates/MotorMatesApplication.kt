package com.example.motormates

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp


private const val EMULATOR_HOST = "10.0.2.2"

@HiltAndroidApp
class MotorMatesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // En debug la app habla con el Firebase Local Emulator Suite
        // (firebase emulators:start) en vez del proyecto real. Se hace aquí
        // porque useEmulator() solo vale si se llama antes de la primera
        // lectura o escritura: cuando Hilt pida estas instancias (AuthModule,
        // FirestoreModule) recibirá las mismas, ya apuntando al emulador.
        if (BuildConfig.DEBUG) {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, 9099)
            FirebaseFirestore.getInstance("motormatesdb").useEmulator(EMULATOR_HOST, 8080)
        }
    }
}
