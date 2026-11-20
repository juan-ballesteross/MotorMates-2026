package com.example.motormates.data.source

import com.example.motormates.data.dto.PreferencesDto
import com.example.motormates.data.dto.UserPreferencesHolder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Las preferencias viven dentro del documento del usuario (users/{uid}),
 * en el mapa `preferences`, y no en una colección aparte: se leen siempre
 * junto con el perfil y así no hace falta otra regla ni otro documento.
 */
interface PreferencesDataSource {
    /** null si el usuario todavía no tiene preferencias guardadas. */
    suspend fun get(uid: String): PreferencesDto?
    fun observe(uid: String): Flow<PreferencesDto?>
    suspend fun save(uid: String, preferences: PreferencesDto)
}

class FirestorePreferencesDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : PreferencesDataSource {

    private fun userDoc(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    override suspend fun get(uid: String): PreferencesDto? =
        userDoc(uid).get().await().toObject(UserPreferencesHolder::class.java)?.preferences

    override fun observe(uid: String): Flow<PreferencesDto?> = callbackFlow {
        val registration = userDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            // Igual que observeUser: un primer evento vacío desde el caché
            // no es "no tiene preferencias", es que todavía no se sabe.
            if (snapshot != null && !snapshot.exists() && snapshot.metadata.isFromCache) {
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(UserPreferencesHolder::class.java)?.preferences)
        }
        awaitClose { registration.remove() }
    }

    /**
     * mergeFields("preferences") reemplaza el mapa completo (así una lista
     * que se vació queda vacía) sin tocar el resto del perfil.
     */
    override suspend fun save(uid: String, preferences: PreferencesDto) {
        userDoc(uid)
            .set(mapOf(PREFERENCES_FIELD to preferences.toMap()), SetOptions.mergeFields(PREFERENCES_FIELD))
            .await()
    }

    private companion object {
        const val PREFERENCES_FIELD = "preferences"
    }
}
