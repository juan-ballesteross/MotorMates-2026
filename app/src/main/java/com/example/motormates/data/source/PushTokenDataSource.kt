package com.example.motormates.data.source

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

interface PushTokenDataSource {
    suspend fun saveToken(uid: String, token: String)
    suspend fun deleteToken(uid: String, token: String)
}

/**
 * Los tokens de FCM de cada usuario, en users/{uid}/fcmTokens/{token}.
 *
 * Esta fuente no tiene gemela para el backend propio y no pasa por
 * DataSourceModule: el token solo le sirve a quien envía el push, que es la
 * Cloud Function, y esa es infraestructura de Firebase igual que Auth y las
 * imágenes (ver DataBackend.kt). Así que se guarda siempre en Firestore,
 * valga ACTIVE_BACKEND lo que valga.
 *
 * Es una subcolección y no un campo del perfil porque una misma cuenta puede
 * estar en varios dispositivos: el id del documento es el token, así no se
 * duplica, y un token muerto se borra con un delete puntual.
 */
class FirestorePushTokenDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : PushTokenDataSource {

    private fun tokenRef(uid: String, token: String) =
        firestore.collection(USERS_COLLECTION).document(uid)
            .collection(FCM_TOKENS_COLLECTION).document(token)

    override suspend fun saveToken(uid: String, token: String) {
        // set y no add: si el token ya estaba, solo se refresca la fecha.
        tokenRef(uid, token).set(mapOf("createdAt" to System.currentTimeMillis())).await()
    }

    override suspend fun deleteToken(uid: String, token: String) {
        tokenRef(uid, token).delete().await()
    }
}
