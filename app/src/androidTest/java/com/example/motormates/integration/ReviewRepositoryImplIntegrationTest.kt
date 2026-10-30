package com.example.motormates.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.repository.NotificationRepositoryImpl
import com.example.motormates.data.repository.ReviewRepositoryImpl
import com.example.motormates.data.source.FirebaseAuthDataSource
import com.example.motormates.data.source.FirestoreNotificationDataSource
import com.example.motormates.data.source.FirestoreReviewDataSource
import com.example.motormates.data.source.FirestoreReviewRealtimeDataSource
import com.example.motormates.data.source.FirestoreUserDataSource
import com.example.motormates.data.source.FirestoreVehicleDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ReviewRepositoryImpl armado con todas sus piezas reales (Auth, perfiles,
 * vehículos, reseñas y notificaciones) sobre el emulador. A diferencia de
 * ReviewRepositoryImplTest, aquí no hay fakes: se prueba que las capas
 * encajen. Requiere npm run serve en functions/.
 */
@RunWith(AndroidJUnit4::class)
class ReviewRepositoryImplIntegrationTest {

    private val firestore = FirebaseEmulator.firestore
    private val userDataSource = FirestoreUserDataSource(firestore)
    private val vehicleDataSource = FirestoreVehicleDataSource(firestore)
    private val notificationDataSource = FirestoreNotificationDataSource(firestore)
    private val authDataSource = FirebaseAuthDataSource(FirebaseEmulator.auth)

    private val repository = ReviewRepositoryImpl(
        remoteDataSource = FirestoreReviewDataSource(firestore),
        realtimeDataSource = FirestoreReviewRealtimeDataSource(firestore),
        vehicleDataSource = vehicleDataSource,
        userDataSource = userDataSource,
        authDataSource = authDataSource,
        notificationRepository = NotificationRepositoryImpl(notificationDataSource, userDataSource, vehicleDataSource)
    )

    private lateinit var uid: String

    @Before
    fun setUp() = runTest {
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
        FirebaseEmulator.seedVehicle(id = "1", brand = "Porsche", model = "911 GT3")
        uid = FirebaseEmulator.signInAs(JOSE_EMAIL)
        userDataSource.createUser(UserDto(id = uid, fullName = "Jose Perfil", email = JOSE_EMAIL, username = "jose"))
    }

    @After
    fun tearDown() = runTest {
        FirebaseEmulator.auth.signOut()
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
    }

    @Test
    fun createReviewUsaElPerfilDelAutorYElVehiculo() = runTest {
        val review = repository.createReview(vehicleId = "1", rating = 5, comment = "Brutal").getOrThrow()

        assertEquals(uid, review.userId)
        assertEquals("Jose Perfil", review.author?.fullName)
        assertEquals("Porsche", review.vehicle?.brand)
        assertEquals("911 GT3", review.vehicle?.model)
        assertEquals(listOf(review.id), repository.getReviewsByVehicle("1").getOrThrow().map { it.id })
    }

    @Test
    fun createReviewSinSesionFallaYNoEscribe() = runTest {
        FirebaseEmulator.auth.signOut()

        val result = repository.createReview(vehicleId = "1", rating = 4, comment = null)

        assertEquals("Debes iniciar sesión para publicar una reseña", result.exceptionOrNull()?.message)
        FirebaseEmulator.signInAs(JOSE_EMAIL) // las reglas exigen sesión para leer
        assertTrue(repository.getReviewsByVehicle("1").getOrThrow().isEmpty())
    }

    @Test
    fun createReviewConCalificacionInvalidaFallaYNoEscribe() = runTest {
        val result = repository.createReview(vehicleId = "1", rating = 7, comment = null)

        assertEquals(INVALID_RATING, result.exceptionOrNull()?.message)
        assertTrue(repository.getReviewsByVehicle("1").getOrThrow().isEmpty())
    }

    @Test
    fun createReviewEnVehiculoInexistenteDevuelveMensajeEnEspanol() = runTest {
        val result = repository.createReview(vehicleId = "999", rating = 4, comment = null)

        assertEquals("El usuario o el vehículo ya no existe", result.exceptionOrNull()?.message)
    }

    @Test
    fun getReviewsByVehicleMuestraElNombreActualDelAutor() = runTest {
        repository.createReview(vehicleId = "1", rating = 5, comment = null).getOrThrow()

        // La reseña guardó "Jose Perfil"; el repositorio debe mostrar el nombre nuevo.
        userDataSource.updateUser(uid, fullName = "Jose Renombrado", username = "jose", location = "", bio = "")
        val reviews = repository.getReviewsByVehicle("1").getOrThrow()

        assertEquals("Jose Renombrado", reviews.single().author?.fullName)
    }

    @Test
    fun getReviewsByUserDevuelveLasReseniasDelUsuario() = runTest {
        FirebaseEmulator.seedVehicle(id = "2", brand = "Toyota", model = "Land Cruiser")
        repository.createReview(vehicleId = "1", rating = 5, comment = null).getOrThrow()
        repository.createReview(vehicleId = "2", rating = 3, comment = null).getOrThrow()

        val reviews = repository.getReviewsByUser(uid).getOrThrow()

        assertEquals(setOf("1", "2"), reviews.map { it.vehicleId }.toSet())
        assertTrue(reviews.all { it.createdAt != null })
    }

    @Test
    fun updateReviewDevuelveLaReseniaConLosCambios() = runTest {
        val created = repository.createReview(vehicleId = "1", rating = 5, comment = "Brutal").getOrThrow()

        val updated = repository.updateReview(created.id, rating = 3, comment = "Ya no tanto").getOrThrow()

        assertEquals(created.id, updated.id)
        assertEquals(3, updated.rating)
        assertEquals("Ya no tanto", updated.comment)
    }

    @Test
    fun updateReviewInexistenteDevuelveMensajeDeNoEncontrada() = runTest {
        val result = repository.updateReview("no-existe", rating = 3, comment = null)

        assertEquals("Esta reseña ya no existe", result.exceptionOrNull()?.message)
    }

    @Test
    fun deleteReviewLaQuitaDeLaLista() = runTest {
        val created = repository.createReview(vehicleId = "1", rating = 5, comment = null).getOrThrow()

        assertTrue(repository.deleteReview(created.id).isSuccess)

        assertTrue(repository.getReviewsByVehicle("1").getOrThrow().isEmpty())
    }

    @Test
    fun createReviewNotificaAQuienYaHabiaReseniadoElVehiculo() = runTest {
        val otroUid = FirebaseEmulator.signInAs(OTRO_EMAIL)
        userDataSource.createUser(UserDto(id = otroUid, fullName = "Otro", email = OTRO_EMAIL, username = "otro"))
        repository.createReview(vehicleId = "1", rating = 4, comment = null).getOrThrow()

        FirebaseEmulator.signInAs(JOSE_EMAIL)
        val nueva = repository.createReview(vehicleId = "1", rating = 5, comment = null).getOrThrow()

        // Solo el dueño puede leer su bandeja.
        FirebaseEmulator.signInAs(OTRO_EMAIL)
        val notifications = notificationDataSource.getNotifications(otroUid)
        val notification = notifications.single()
        assertEquals("REVIEW", notification.type)
        assertEquals(uid, notification.actorId)
        assertEquals(nueva.id, notification.reviewId)
    }

    @Test
    fun observeReviewsByVehicleEmiteLaReseniaRecienCreada() = runTest {
        val created = repository.createReview(vehicleId = "1", rating = 5, comment = null).getOrThrow()

        // Tope de 10 s reales (Dispatchers.Default): dentro de runTest el tiempo es
        // virtual y withTimeout saltaría antes de que Firestore alcance a emitir.
        val reviews = withContext(Dispatchers.Default) {
            withTimeout(10_000) {
                repository.observeReviewsByVehicle("1")
                    .first { result -> result.getOrNull()?.any { it.id == created.id } == true }
            }
        }.getOrThrow()

        assertEquals("Jose Perfil", reviews.single().author?.fullName)
    }

    private companion object {
        const val JOSE_EMAIL = "jose@motormates.com"
        const val OTRO_EMAIL = "otro@motormates.com"
        const val INVALID_RATING = "La calificación debe ser un número entre 0 y 5"
    }
}
