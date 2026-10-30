package com.example.motormates.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.source.FirestoreReviewDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FirestoreReviewDataSource contra el emulador de Firestore real: se
 * prueban las consultas, las escrituras y que las reglas de
 * firestore.rules se apliquen. Requiere npm run serve en functions/.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreReviewDataSourceIntegrationTest {

    private val firestore = FirebaseEmulator.firestore
    private lateinit var dataSource: FirestoreReviewDataSource
    private lateinit var uid: String

    @Before
    fun setUp() = runTest {
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
        uid = FirebaseEmulator.signInAs("jose@motormates.com")
        dataSource = FirestoreReviewDataSource(firestore)
    }

    @After
    fun tearDown() = runTest {
        FirebaseEmulator.auth.signOut()
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
    }

    @Test
    fun createReviewDevuelveIdGeneradoYElDocumentoExiste() = runTest {
        val created = create(newReview())

        assertTrue(created.id.isNotBlank())
        val snapshot = firestore.collection("reviews").document(created.id).get().await()
        assertTrue(snapshot.exists())
    }

    @Test
    fun createReviewGuardaAutorYVehiculoDesnormalizados() = runTest {
        val created = create(newReview(comment = "Brutal"))

        val saved = firestore.collection("reviews").document(created.id).get().await()
            .toObject(ReviewFirestoreDto::class.java)!!
        assertEquals(uid, saved.userId)
        assertEquals("Brutal", saved.comment)
        assertEquals("Jose Perfil", saved.user?.fullName)
        assertEquals("Porsche", saved.vehicle?.brand)
        assertEquals("911 GT3", saved.vehicle?.model)
        assertTrue(saved.createdAt > 0)
        assertEquals(saved.createdAt, saved.updatedAt)
    }

    @Test
    fun getReviewsByVehicleTraeSoloLasDeEseVehiculo() = runTest {
        val porsche = create(newReview(vehicleId = "1"))
        create(newReview(vehicleId = "2"))

        val reviews = dataSource.getReviewsByVehicle("1")

        assertEquals(listOf(porsche.id), reviews.map { it.toDomain().id })
    }

    @Test
    fun getReviewsByVehicleOrdenaDeLaMasRecienteALaMasVieja() = runTest {
        val vieja = create(newReview(comment = "primera"))
        Thread.sleep(20) // createdAt es en milisegundos: se asegura que no empaten
        val nueva = create(newReview(comment = "segunda"))

        val reviews = dataSource.getReviewsByVehicle("1")

        assertEquals(listOf(nueva.id, vieja.id), reviews.map { it.toDomain().id })
    }

    @Test
    fun getReviewsByUserTraeSoloLasDelUsuario() = runTest {
        create(newReview(vehicleId = "1"))
        create(newReview(vehicleId = "2"))

        val propias = dataSource.getReviewsByUser(uid)
        val ajenas = dataSource.getReviewsByUser("otro-usuario")

        assertEquals(2, propias.size)
        assertTrue(propias.all { it.toDomain().userId == uid })
        assertTrue(ajenas.isEmpty())
    }

    @Test
    fun getReviewsByVehicleSinReseniasDevuelveListaVacia() = runTest {
        assertTrue(dataSource.getReviewsByVehicle("1").isEmpty())
    }

    @Test
    fun updateReviewSoloComentarioConservaElRating() = runTest {
        val created = create(newReview(rating = 4, comment = "Bien"))

        val updated = dataSource.updateReview(created.id, rating = null, comment = "Mejor de lo que pensé")
            .toDomain()

        assertEquals(4, updated.rating)
        assertEquals("Mejor de lo que pensé", updated.comment)
    }



    @Test
    fun deleteReviewBorraElDocumento() = runTest {
        val created = create(newReview())

        dataSource.deleteReview(created.id)

        val snapshot = firestore.collection("reviews").document(created.id).get().await()
        assertFalse(snapshot.exists())
        assertNull(snapshot.toObject(ReviewFirestoreDto::class.java))
        assertTrue(dataSource.getReviewsByVehicle("1").isEmpty())
    }

    @Test
    fun createReviewANombreDeOtroUsuarioLoRechazanLasReglas() = runTest {
        try {
            dataSource.createReview(newReview(userId = "otro-usuario"))
            fail("Las reglas debieron rechazar la escritura")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
        assertTrue(dataSource.getReviewsByVehicle("1").isEmpty())
    }

    /** createReview devuelve la interfaz genérica; para leer el id hace falta el DTO de Firestore. */
    private suspend fun create(review: NewReview) = dataSource.createReview(review) as ReviewFirestoreDto

    private fun newReview(
        userId: String = uid,
        vehicleId: String = "1",
        rating: Int = 5,
        comment: String? = null
    ) = NewReview(
        userId = userId,
        vehicleId = vehicleId,
        rating = rating,
        comment = comment,
        authorName = "Jose Perfil",
        authorAvatarUrl = null,
        vehicleBrand = "Porsche",
        vehicleModel = "911 GT3"
    )
}
