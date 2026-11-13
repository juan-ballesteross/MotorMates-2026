package com.example.motormates.e2e

import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.ReviewLocationSnapshot
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.ReviewVehicleSnapshot
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.source.FirestoreUserDataSource
import com.example.motormates.integration.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Datos de partida de las pruebas E2E.
 *
 * Las pruebas crean y borran lo suyo (propiedad de autonomía de la guía):
 * no se apoyan en el seed de demo (seed/seed.js) ni en el autoseed de la app
 * (FirestoreSeeder.seedIfEmpty, que lanza FeedViewModel). De hecho, al
 * sembrar el vehículo aquí la colección "vehicles" deja de estar vacía y ese
 * autoseed no hace nada, así que lo que se ve en pantalla es exactamente
 * esto y nada más.
 *
 * Se reutiliza integration.FirebaseEmulator: ese objeto habla con el
 * emulador por un FirebaseApp aparte y por REST, así que puede limpiar y
 * escribir sin pelearse con las instancias que usa la app.
 */
object E2ESeed {

    // La publicación: un solo vehículo, así "la primera publicación" del
    // feed no es ambigua.
    const val VEHICLE_ID = "1"
    const val BRAND = "Porsche"
    const val MODEL = "911 GT3"
    const val YEAR = 2024
    const val CATEGORY = "Deportivo"
    const val POTENCIA = "502 hp"
    const val ACELERACION = "3.4 s"
    const val VELOCIDAD_MAXIMA = "318 km/h"
    const val TRACCION = "RWD"

    /** El autor de las reseñas: es a quien se sigue en la segunda prueba. */
    const val AUTHOR_EMAIL = "otro@motormates.com"
    const val AUTHOR_NAME = "Otro Usuario"
    const val AUTHOR_USERNAME = "otro"
    const val AUTHOR_LOCATION = "Bogotá, Colombia"
    const val AUTHOR_BIO = "Fan de los deportivos y los track days."

    /** El usuario que ya existe y entra por el login. */
    const val MEMBER_EMAIL = "yo@motormates.com"
    const val MEMBER_NAME = "Yo Mismo"
    const val MEMBER_USERNAME = "yo"

    /**
     * Seis reseñas y no una: el botón "Ver todas" del detalle solo aparece
     * si hay más de las que caben en línea (entre 2 y 5 según el alto de la
     * pantalla), y la prueba tiene que llegar de verdad a la sección de
     * reseñas completa.
     */
    const val REVIEWS_COUNT = 6

    /**
     * Las reseñas se ordenan en memoria por createdAt descendente
     * (ReviewRealtimeDataSource), así que la más reciente es "review_1" y el
     * "primer comentario" de la prueba es determinista.
     */
    const val FIRST_REVIEW_ID = "review_1"

    private const val REVIEWS_COLLECTION = "reviews"
    private const val ONE_HOUR_MS = 60 * 60 * 1000L

    /** Punto de partida de las ubicaciones de prueba (centro de Bogotá). */
    private const val MAP_LAT = 4.6280
    private const val MAP_LNG = -74.0643
    private const val MAP_OFFSET = 0.01

    /** El lugar que las pruebas esperan ver junto al "hace X" de la reseña. */
    const val REVIEW_PLACE_NAME = "Chapinero, Bogotá"

    private val users by lazy { FirestoreUserDataSource(FirebaseEmulator.firestore) }

    /**
     * Deja la base con el vehículo, el autor y sus seis reseñas, y sin
     * ninguna sesión abierta. Devuelve el uid del autor.
     */
    suspend fun seedAll(): String {
        reset()

        FirebaseEmulator.seedVehicle(
            id = VEHICLE_ID,
            brand = BRAND,
            model = MODEL,
            year = YEAR,
            category = CATEGORY,
            potencia = POTENCIA,
            aceleracion = ACELERACION,
            velocidadMaxima = VELOCIDAD_MAXIMA,
            traccion = TRACCION
        )

        // Las reglas exigen que la reseña se publique a nombre de quien está
        // autenticado (userId == request.auth.uid), por eso se abre la sesión
        // del autor para escribirlas.
        val authorUid = FirebaseEmulator.signInAs(AUTHOR_EMAIL)
        users.createUser(
            UserDto(
                id = authorUid,
                fullName = AUTHOR_NAME,
                email = AUTHOR_EMAIL,
                username = AUTHOR_USERNAME,
                location = AUTHOR_LOCATION,
                bio = AUTHOR_BIO,
                createdAt = System.currentTimeMillis()
            )
        )
        seedReviews(authorUid)
        FirebaseEmulator.auth.signOut()

        return authorUid
    }

    /**
     * El usuario que hace login en la segunda prueba. Necesita cuenta en Auth
     * **y** perfil en Firestore: LoginViewModel cierra la sesión si el perfil
     * no está (una cuenta sin perfil es de una versión anterior de la app).
     */
    suspend fun seedMember(): String {
        val uid = FirebaseEmulator.signInAs(MEMBER_EMAIL)
        users.createUser(
            UserDto(
                id = uid,
                fullName = MEMBER_NAME,
                email = MEMBER_EMAIL,
                username = MEMBER_USERNAME,
                createdAt = System.currentTimeMillis()
            )
        )
        FirebaseEmulator.auth.signOut()
        // La sesión de la app se guarda en el dispositivo: si queda abierta,
        // el splash entra directo al Feed y la prueba nunca ve el login.
        FirebaseAuth.getInstance().signOut()
        return uid
    }

    /** Deja todo como estaba: sin datos y sin sesiones. */
    suspend fun reset() {
        FirebaseEmulator.auth.signOut()
        FirebaseAuth.getInstance().signOut()
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
    }

    private suspend fun seedReviews(authorUid: String) {
        val now = System.currentTimeMillis()
        repeat(REVIEWS_COUNT) { index ->
            // review_1 es la más reciente; cada siguiente, una hora más vieja.
            val createdAt = now - index * ONE_HOUR_MS
            val review = ReviewFirestoreDto(
                rating = 5,
                comment = "Reseña de prueba numero ${index + 1}",
                userId = authorUid,
                vehicleId = VEHICLE_ID,
                createdAt = createdAt,
                updatedAt = createdAt,
                likesCount = 0,
                user = ReviewUserSnapshot(fullName = AUTHOR_NAME),
                vehicle = ReviewVehicleSnapshot(brand = BRAND, model = MODEL),
                // Todas con ubicación, y todas de hace menos de 24 h (una
                // hora de diferencia entre cada una), así la pestaña del mapa
                // tiene marcadores que mostrar. Se separan un poco entre sí
                // para que no queden una encima de otra.
                location = ReviewLocationSnapshot(
                    lat = MAP_LAT + index * MAP_OFFSET,
                    lng = MAP_LNG - index * MAP_OFFSET,
                    // Quemado y no geocodificado: las pruebas no deben
                    // depender de la red ni del servicio de Google.
                    placeName = REVIEW_PLACE_NAME
                )
            )
            FirebaseEmulator.firestore
                .collection(REVIEWS_COLLECTION)
                .document("review_${index + 1}")
                .set(review)
                .await()
        }
    }
}
