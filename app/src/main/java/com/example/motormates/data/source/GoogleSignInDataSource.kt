package com.example.motormates.data.source

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.IOException
import javax.inject.Inject
import javax.inject.Qualifier

/**
 * El selector de cuentas de Google, detrás de una interfaz.
 *
 * No pasa por el pick() de DataSourceModule, igual que
 * DeviceLocationDataSource: esto no es un backend, es una capacidad del
 * teléfono, y es la misma con Firestore o con el backend propio. Se declara
 * como interfaz para que el ViewModel se pueda probar con MockK sin tocar
 * Credential Manager ni Play Services.
 */
interface GoogleSignInDataSource {

    /**
     * Abre el selector de cuentas de Google y devuelve el idToken de la que
     * se eligió, para dárselo a Firebase.
     *
     * @param activityContext contexto de la **Activity**. El selector es una
     *   UI y necesita una Activity para montarse, así que no sirve el de
     *   aplicación. Llega por parámetro y no por el constructor justamente
     *   para no guardarlo: se usa dentro de la llamada y se suelta.
     * @return el idToken, o null si el usuario cerró el selector sin elegir.
     *   Cancelar no es un error: es una respuesta válida y la pantalla no
     *   debe mostrar nada.
     * @throws GoogleSignInException si falló de verdad.
     */
    suspend fun idToken(activityContext: Context): String?
}

/**
 * Por qué falló el selector, ya clasificado.
 *
 * La causa original viaja en `cause` en vez de perderse: el mensaje que ve el
 * usuario lo arma el repositorio a partir del `reason`, pero en el log tiene
 * que quedar qué pasó de verdad.
 */
class GoogleSignInException(
    val reason: Reason,
    cause: Throwable? = null
) : Exception(reason.name, cause) {

    enum class Reason {
        /** No hay ninguna cuenta de Google agregada en el dispositivo. */
        NO_ACCOUNTS,

        /**
         * Google rechazó la petición: web client id equivocado, o el SHA-1 de
         * la firma de la app no está registrado en Firebase. Es un error de
         * configuración del proyecto, no algo que el usuario pueda arreglar.
         */
        MISCONFIGURED,

        NETWORK,

        UNKNOWN
    }
}

/** El web client id de Firebase, ya resuelto desde los recursos. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleWebClientId

class CredentialManagerGoogleSignInDataSource @Inject constructor(
    @GoogleWebClientId private val webClientId: String
) : GoogleSignInDataSource {

    override suspend fun idToken(activityContext: Context): String? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        val credential = try {
            CredentialManager.create(activityContext)
                .getCredential(context = activityContext, request = request)
                .credential
        } catch (e: GetCredentialCancellationException) {
            // El usuario cerró el selector. Se distingue por el tipo de
            // excepción y no por el texto del mensaje, que cambia entre
            // versiones de Play Services y entre idiomas.
            return null
        } catch (e: NoCredentialException) {
            throw GoogleSignInException(GoogleSignInException.Reason.NO_ACCOUNTS, e)
        } catch (e: GetCredentialException) {
            // Lo más común acá es el SHA-1 sin registrar en Firebase.
            throw GoogleSignInException(GoogleSignInException.Reason.MISCONFIGURED, e)
        } catch (e: IOException) {
            throw GoogleSignInException(GoogleSignInException.Reason.NETWORK, e)
        }

        // Se pidió una credencial de Google, así que debería llegar una. Si
        // llega otra cosa es un error y no un "canceló": devolver null haría
        // que la pantalla se quede sin loading y sin mensaje, o sea que el
        // botón no haría nada visible.
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleSignInException(GoogleSignInException.Reason.UNKNOWN)
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: Exception) {
            throw GoogleSignInException(GoogleSignInException.Reason.UNKNOWN, e)
        }
    }
}
