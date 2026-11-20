package com.example.motormates

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.motormates.notifications.AppForegroundState
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient


private const val EMULATOR_HOST = "10.0.2.2"
private const val IMAGE_USER_AGENT = "MotorMates/1.0 (proyecto academico; Android)"

@HiltAndroidApp
class MotorMatesApplication : Application(), ImageLoaderFactory {

    /** Para saber si notificar un resultado de AI (solo si no se está mirando). */
    @Inject
    lateinit var foregroundState: AppForegroundState

    /**
     * Cargador de imágenes de toda la app (Coil). Wikimedia, de donde salen
     * casi todas las fotos de vehículos y piezas, rechaza las peticiones con
     * un User-Agent genérico (el "okhttp/4.x" por defecto), y entonces las
     * fotos quedaban en blanco. Se identifica la app, sin datos personales.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder().header("User-Agent", IMAGE_USER_AGENT).build()
                        )
                    }
                    .build()
            }
            .crossfade(true)
            .build()

    override fun onCreate() {
        super.onCreate()
        // App Check antes de cualquier llamada a Firebase: protege la cuota
        // de Gemini (AI Tuning). Mientras no se active "enforcement" en la
        // consola, no bloquea nada; solo adjunta el token a las peticiones.
        AppCheckSetup.install(this)
        foregroundState.register(this)
        // En debug la app habla con el Firebase Local Emulator Suite
        // (firebase emulators:start) en vez del proyecto real. Se hace aquí
        // porque useEmulator() solo vale si se llama antes de la primera
        // lectura o escritura: cuando Hilt pida estas instancias (AuthModule,
        // FirestoreModule) recibirá las mismas, ya apuntando al emulador.
        // Se agrego para que funcione en la tablet
        if (BuildConfig.DEBUG && BuildConfig.CONNECT_TO_FIREBASE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, 9099)
            FirebaseFirestore.getInstance("motormatesdb").useEmulator(EMULATOR_HOST, 8080)
        }
    }
}
