package com.example.motormates.integration

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Acceso al Firebase Emulator Suite para las pruebas de integración
 * (npm run serve en functions/: Firestore 8080 y Auth 9099).
 *
 * Usa un FirebaseApp propio y no el de por defecto: MotorMatesApplication
 * ya tocó las instancias por defecto al arrancar, y useEmulator() lanza
 * IllegalStateException si se llama después de la primera operación.
 */
object FirebaseEmulator {

    // 10.0.2.2 es el localhost del PC visto desde el emulador de Android.
    private const val HOST = "10.0.2.2"
    private const val FIRESTORE_PORT = 8080
    private const val AUTH_PORT = 9099
    private const val DATABASE_ID = "motormatesdb"
    private const val PASSWORD = "secreto123"

    private val app: FirebaseApp by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "integration-test")
    }

    private val projectId get() = requireNotNull(app.options.projectId)

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(app, DATABASE_ID).apply { useEmulator(HOST, FIRESTORE_PORT) }
    }

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance(app).apply { useEmulator(HOST, AUTH_PORT) }
    }

    /** Borra todos los documentos de motormatesdb en el emulador. */
    suspend fun clearFirestore() =
        request("DELETE", "http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/$DATABASE_ID/documents")

    /** Borra todas las cuentas del emulador de Auth. */
    suspend fun clearAuth() =
        request("DELETE", "http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts")

    /** Crea la cuenta (si no existe) y deja la sesión abierta con ella. Devuelve el uid. */
    suspend fun signInAs(email: String): String {
        auth.signOut()
        val result = runCatching { auth.createUserWithEmailAndPassword(email, PASSWORD).await() }
            .getOrElse { auth.signInWithEmailAndPassword(email, PASSWORD).await() }
        return requireNotNull(result.user).uid
    }

    /**
     * Las reglas no dejan escribir en "vehicles" desde la app, así que se
     * siembra por REST con el token "owner", que en el emulador se salta
     * las reglas.
     */
    suspend fun seedVehicle(id: String, brand: String, model: String, year: Int = 2024) {
        val body = """
            {"fields": {
              "brand": {"stringValue": "$brand"},
              "model": {"stringValue": "$model"},
              "year": {"integerValue": "$year"}
            }}
        """.trimIndent()
        request(
            "POST",
            "http://$HOST:$FIRESTORE_PORT/v1/projects/$projectId/databases/$DATABASE_ID/documents/vehicles?documentId=$id",
            body
        )
    }

    private suspend fun request(method: String, url: String, body: String? = null) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer owner")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            check(code in 200..299) { "El emulador respondió $code a $method $url. ¿Está corriendo npm run serve?" }
        } finally {
            connection.disconnect()
        }
    }
}
