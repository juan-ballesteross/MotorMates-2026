package com.example.motormates.data.repository

import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.PushTokenDataSource
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

interface PushTokenRepository {
    /**
     * Registra el token de este dispositivo para la sesión actual. Se llama
     * en cada arranque con sesión abierta, no solo al iniciar sesión: FCM
     * puede rotar el token (reinstalación, borrado de datos, otro dispositivo).
     */
    suspend fun registerCurrentDevice()

    /** Borra el token antes de cerrar sesión, para que no le sigan llegando avisos de otra cuenta. */
    suspend fun unregisterCurrentDevice()

    /** Para onNewToken, que llega desde el servicio sin pasar por la UI. */
    suspend fun saveToken(token: String)
}

/**
 * Mismo criterio que NotificationRepository.notify: ningún error se propaga.
 * Que no se pueda registrar el token no debe romper el login ni el logout —
 * como mucho el usuario se queda sin push hasta el siguiente arranque.
 *
 * Depende de AuthDataSource y no de AuthRepository porque AuthRepositoryImpl
 * usa este repositorio al cerrar sesión: por ahí se haría un ciclo.
 */
class PushTokenRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource,
    private val pushTokenDataSource: PushTokenDataSource
) : PushTokenRepository {

    override suspend fun registerCurrentDevice() {
        runCatching {
            val uid = authDataSource.getCurrentUser()?.id ?: return
            pushTokenDataSource.saveToken(uid, currentToken())
        }
    }

    override suspend fun unregisterCurrentDevice() {
        runCatching {
            // Tiene que correr antes del signOut: las reglas solo dejan al
            // dueño borrar sus tokens, y sin sesión ya no hay dueño.
            val uid = authDataSource.getCurrentUser()?.id ?: return
            pushTokenDataSource.deleteToken(uid, currentToken())
        }
    }

    override suspend fun saveToken(token: String) {
        runCatching {
            val uid = authDataSource.getCurrentUser()?.id ?: return
            pushTokenDataSource.saveToken(uid, token)
        }
    }

    private suspend fun currentToken(): String =
        FirebaseMessaging.getInstance().token.await()
}
