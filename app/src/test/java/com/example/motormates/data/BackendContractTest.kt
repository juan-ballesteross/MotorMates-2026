package com.example.motormates.data

import com.example.motormates.data.model.CURRENT_USER_ID
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.model.toUserProfile
import com.example.motormates.data.model.toUserReviewUi
import com.example.motormates.data.remote.ReviewApiService
import com.example.motormates.data.remote.ReviewRetrofitDataSource
import com.example.motormates.data.remote.UserApiService
import com.example.motormates.data.remote.UserRetrofitDataSource
import com.example.motormates.data.remote.VehicleApiService
import com.example.motormates.data.remote.VehicleRetrofitDataSource
import com.example.motormates.data.repository.ReviewRepositoryImpl
import com.example.motormates.data.repository.UserRepositoryImpl
import com.example.motormates.data.repository.VehicleRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Prueba de contrato contra el backend real: verifica que los DTOs, los
 * mappers y el manejo de errores de los repositorios calcen con lo que
 * responde el servidor de verdad, que es donde más fácil se rompe algo
 * (un campo renombrado, un anidado que no llega, un 204 sin cuerpo).
 *
 * Corre sobre localhost y no sobre 10.0.2.2, porque 10.0.2.2 es el alias
 * que usa el emulador para llegar al host; desde la JVM no existe.
 *
 * Si el backend no está levantado, los tests se omiten (assumeTrue) en vez
 * de fallar: así no se rompe el build de alguien que no lo tenga corriendo.
 * Para correrlos: npm run dev en Backend-MotorMates.
 */
class BackendContractTest {

    private companion object {
        const val TEST_BASE_URL = "http://localhost:3000/"
    }

    private lateinit var vehicleRepository: VehicleRepositoryImpl
    private lateinit var reviewRepository: ReviewRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl
    private var backendUp = false

    @Before
    fun setUp() {
        val retrofit = Retrofit.Builder()
            .baseUrl(TEST_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        vehicleRepository = VehicleRepositoryImpl(
            VehicleRetrofitDataSource(retrofit.create(VehicleApiService::class.java))
        )
        reviewRepository = ReviewRepositoryImpl(
            ReviewRetrofitDataSource(retrofit.create(ReviewApiService::class.java))
        )
        userRepository = UserRepositoryImpl(
            UserRetrofitDataSource(retrofit.create(UserApiService::class.java))
        )

        backendUp = runBlocking { vehicleRepository.getVehicles().isSuccess }
    }

    @Test
    fun `getVehicles devuelve la lista sembrada y la mapea a dominio`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicles = vehicleRepository.getVehicles().getOrThrow()

        assertTrue("Se esperaban vehículos sembrados", vehicles.isNotEmpty())
        val porsche = vehicles.first { it.brand == "Porsche" }
        assertEquals("911 GT3", porsche.model)
        assertEquals(2024, porsche.year)
        assertEquals("Deportivo", porsche.category)
        assertNotNull("La semilla del backend trae imagen", porsche.imageUrl)
        // Las especificaciones llegan ya formateadas con su unidad desde la BD.
        assertEquals("502 hp", porsche.potencia)
        assertEquals("3.4 s", porsche.aceleracion)
        assertEquals("318 km/h", porsche.velocidadMaxima)
        assertEquals("RWD", porsche.traccion)
    }

    @Test
    fun `getVehicleById con id inexistente devuelve failure con mensaje en espanol`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val result = vehicleRepository.getVehicleById(999_999)

        assertTrue("Un 404 debe llegar como failure", result.isFailure)
        assertEquals(
            "Este vehículo no existe o fue eliminado",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `las resenas de un vehiculo traen el autor anidado`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicleId = vehicleRepository.getVehicles().getOrThrow().first().id
        val created = reviewRepository
            .createReview(CURRENT_USER_ID, vehicleId, rating = 5, comment = "Prueba de contrato")
            .getOrThrow()

        try {
            val reviews = reviewRepository.getReviewsByVehicle(vehicleId).getOrThrow()
            val mine = reviews.first { it.id == created.id }

            // El backend incluye user con solo {id, fullName}.
            assertNotNull("Debía venir el autor anidado", mine.author)
            assertEquals(CURRENT_USER_ID, mine.author?.id)
            assertTrue("El autor debía tener nombre", mine.author?.fullName?.isNotBlank() == true)

            // El mapper a UI marca la reseña como propia y recorta el rating.
            val ui = mine.toReviewUi()
            assertTrue("Debía marcarse como mía", ui.isMine)
            assertEquals(mine.author?.fullName, ui.nombreUsuario)
            assertTrue("Debía calcular el tiempo relativo", ui.tiempoTexto.isNotBlank())
        } finally {
            reviewRepository.deleteReview(created.id)
        }
    }

    @Test
    fun `las resenas de un usuario traen el vehiculo anidado`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicle = vehicleRepository.getVehicles().getOrThrow().first()
        val created = reviewRepository
            .createReview(CURRENT_USER_ID, vehicle.id, rating = 4, comment = null)
            .getOrThrow()

        try {
            val reviews = reviewRepository.getReviewsByUser(CURRENT_USER_ID).getOrThrow()
            val mine = reviews.first { it.id == created.id }

            // El backend incluye vehicle con solo {id, brand, model}.
            assertNotNull("Debía venir el vehículo anidado", mine.vehicle)
            assertEquals(vehicle.brand, mine.vehicle?.brand)
            // comment viaja null y el mapper lo convierte en cadena vacía.
            assertEquals(null, mine.comment)
            assertEquals("", mine.toUserReviewUi().comment)
            assertEquals("${vehicle.brand} ${vehicle.model}", mine.toUserReviewUi().vehicleName)
        } finally {
            reviewRepository.deleteReview(created.id)
        }
    }

    @Test
    fun `editar una resena permite cambiar rating y borrar el comentario`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicleId = vehicleRepository.getVehicles().getOrThrow().first().id
        val created = reviewRepository
            .createReview(CURRENT_USER_ID, vehicleId, rating = 1, comment = "Texto original")
            .getOrThrow()

        try {
            val updated = reviewRepository
                .updateReview(created.id, rating = 5, comment = "Texto editado")
                .getOrThrow()
            assertEquals(5, updated.rating)
            assertEquals("Texto editado", updated.comment)

            // Mandar cadena vacía debe limpiar el comentario; mandar null lo
            // dejaría intacto, porque Gson omite el campo.
            val cleared = reviewRepository
                .updateReview(created.id, rating = 5, comment = "")
                .getOrThrow()
            assertEquals("", cleared.comment)
        } finally {
            reviewRepository.deleteReview(created.id)
        }
    }

    @Test
    fun `eliminar devuelve success con 204 sin cuerpo y 404 la segunda vez`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicleId = vehicleRepository.getVehicles().getOrThrow().first().id
        val created = reviewRepository
            .createReview(CURRENT_USER_ID, vehicleId, rating = 3, comment = "Para borrar")
            .getOrThrow()

        // 204 con cuerpo vacío: el tipo de retorno Unit debe bastar.
        assertTrue("El 204 debía mapearse a success", reviewRepository.deleteReview(created.id).isSuccess)

        val again = reviewRepository.deleteReview(created.id)
        assertTrue("Borrar dos veces debía fallar", again.isFailure)
        assertEquals("Esta reseña ya no existe", again.exceptionOrNull()?.message)
    }

    @Test
    fun `un rating fuera de rango devuelve el mensaje de validacion`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicleId = vehicleRepository.getVehicles().getOrThrow().first().id
        val result = reviewRepository.createReview(CURRENT_USER_ID, vehicleId, rating = 9, comment = null)

        assertTrue("Un 400 debe llegar como failure", result.isFailure)
        assertEquals(
            "La calificación debe ser un número entre 0 y 5",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `el perfil se sintetiza a partir del nombre y el correo`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val user = userRepository.getUserById(CURRENT_USER_ID).getOrThrow()
        val profile = user.toUserProfile(reviewsCount = 7)

        assertEquals(user.fullName, profile.name)
        assertEquals("@" + user.email.substringBefore("@"), profile.handle)
        assertEquals(7, profile.reviewsCount)
        // El backend no da estos datos: quedan vacíos a propósito.
        assertEquals("", profile.location)
        assertEquals("", profile.bio)
    }

    @Test
    fun `la calificacion del detalle es el promedio real de las resenas`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicle = vehicleRepository.getVehicles().getOrThrow().last()
        val a = reviewRepository
            .createReview(CURRENT_USER_ID, vehicle.id, rating = 2, comment = null).getOrThrow()
        val b = reviewRepository
            .createReview(CURRENT_USER_ID, vehicle.id, rating = 4, comment = null).getOrThrow()

        try {
            val reviews = reviewRepository.getReviewsByVehicle(vehicle.id).getOrThrow()
            val detail = vehicle.toCarDetailUi(reviews)

            assertEquals(reviews.size, detail.numeroResenas)
            assertEquals(
                reviews.map { it.rating }.average().toFloat(),
                detail.calificacion,
                0.001f
            )
            assertEquals(vehicle.brand.uppercase(), detail.marca)
        } finally {
            reviewRepository.deleteReview(a.id)
            reviewRepository.deleteReview(b.id)
        }
    }

    @Test
    fun `un vehiculo sin resenas muestra cero y no divide por cero`() = runBlocking {
        assumeTrue("El backend no está corriendo", backendUp)

        val vehicle = vehicleRepository.getVehicles().getOrThrow().first()
        val detail = vehicle.toCarDetailUi(reviews = emptyList())

        assertEquals(0f, detail.calificacion, 0.001f)
        assertEquals(0, detail.numeroResenas)
    }
}
