package com.example.motormates.data.repository

import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.ReviewVehicleSnapshot
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.dto.VehicleFirestoreDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.remote.datasource.ReviewRemoteDataSource
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.ReviewRealtimeDataSource
import com.example.motormates.data.source.UserDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * ReviewRepositoryImpl aislado con MockK: las fuentes de datos responden lo
 * que se les configura y lo único que se ejecuta de verdad es la lógica del
 * repositorio (mapeos, Result y manejo de excepciones).
 */
class ReviewRepositoryImplMockTest {

    private val remoteDataSource = mockk<ReviewRemoteDataSource>()
    private val realtimeDataSource = mockk<ReviewRealtimeDataSource>()
    private val vehicleDataSource = mockk<VehicleRemoteDataSource>()
    private val userDataSource = mockk<UserDataSource>()
    private val authDataSource = mockk<AuthDataSource>()
    // relaxed: notify no devuelve nada y aquí solo interesa verificar a quién se llamó.
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)

    private val repository = ReviewRepositoryImpl(
        remoteDataSource = remoteDataSource,
        realtimeDataSource = realtimeDataSource,
        vehicleDataSource = vehicleDataSource,
        userDataSource = userDataSource,
        authDataSource = authDataSource,
        notificationRepository = notificationRepository
    )

    private val porscheDto = VehicleFirestoreDto(id = "1", brand = "Porsche", model = "911 GT3", year = 2024)

    // ---------- Mapeos DTO -> dominio ----------

    @Test
    fun `getReviewsByVehicle mapea el DTO de Firestore al modelo de dominio`() = runTest {
        // Arrange
        coEvery { remoteDataSource.getReviewsByVehicle("1") } returns listOf(reviewDto(likesCount = 7))
        coEvery { userDataSource.getUser("uid-jose") } returns null

        // Act
        val review = repository.getReviewsByVehicle("1").getOrThrow().single()

        // Assert
        assertThat(review.id).isEqualTo("r1")
        assertThat(review.rating).isEqualTo(4)
        assertThat(review.comment).isEqualTo("Muy bueno")
        assertThat(review.likesCount).isEqualTo(7)
        assertThat(review.vehicle?.brand).isEqualTo("Porsche")
        assertThat(review.vehicle?.model).isEqualTo("911 GT3")
        assertThat(review.createdAt).isEqualTo("2023-11-14T22:13:20Z")
    }

    @Test
    fun `getReviewsByVehicle con createdAt en cero deja la fecha en null`() = runTest {
        coEvery { remoteDataSource.getReviewsByVehicle("1") } returns listOf(reviewDto(createdAt = 0L))
        coEvery { userDataSource.getUser(any()) } returns null

        val review = repository.getReviewsByVehicle("1").getOrThrow().single()

        assertThat(review.createdAt).isNull()
    }

    @Test
    fun `getReviewsByVehicle reemplaza el autor guardado por el perfil actual`() = runTest {
        coEvery { remoteDataSource.getReviewsByVehicle("1") } returns listOf(reviewDto())
        coEvery { userDataSource.getUser("uid-jose") } returns
            UserDto(id = "uid-jose", fullName = "Jose Renombrado", avatarUrl = "https://nueva")

        val author = repository.getReviewsByVehicle("1").getOrThrow().single().author

        assertThat(author?.fullName).isEqualTo("Jose Renombrado")
        assertThat(author?.avatarUrl).isEqualTo("https://nueva")
    }

    @Test
    fun `getReviewsByVehicle conserva la copia del autor si no se puede leer el perfil`() = runTest {
        coEvery { remoteDataSource.getReviewsByVehicle("1") } returns listOf(reviewDto())
        coEvery { userDataSource.getUser("uid-jose") } throws IOException("sin red")

        val result = repository.getReviewsByVehicle("1")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().single().author?.fullName).isEqualTo("Jose Guardado")
        assertThat(result.getOrThrow().single().author?.avatarUrl).isEqualTo("https://vieja")
    }

    @Test
    fun `getReviewsByUser sin copia del autor arma el autor desde el perfil`() = runTest {
        coEvery { remoteDataSource.getReviewsByUser("uid-jose") } returns listOf(reviewDto(user = null))
        coEvery { userDataSource.getUser("uid-jose") } returns UserDto(id = "uid-jose", fullName = "Jose Perfil")

        val author = repository.getReviewsByUser("uid-jose").getOrThrow().single().author

        assertThat(author?.id).isEqualTo("uid-jose")
        assertThat(author?.fullName).isEqualTo("Jose Perfil")
    }

    // ---------- Errores -> Result.failure con mensaje en español ----------

    @Test
    fun `getReviewsByVehicle sin conexion devuelve el mensaje de sin conexion`() = runTest {
        coEvery { remoteDataSource.getReviewsByVehicle("1") } throws IOException("timeout")

        val result = repository.getReviewsByVehicle("1")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message)
            .isEqualTo("Sin conexión con el servidor, verifica que esté encendido")
    }

    @Test
    fun `updateReview de una resenia borrada devuelve mensaje de no encontrada`() = runTest {
        // Es lo que lanza FirestoreReviewDataSource si el documento ya no existe.
        // (FirebaseFirestoreException.Code no se puede crear en la JVM: usa SparseArray de Android.)
        coEvery { remoteDataSource.updateReview("r1", 3, null) } throws
            NoSuchElementException("No existe la reseña r1")

        val result = repository.updateReview("r1", rating = 3, comment = null)

        assertThat(result.exceptionOrNull()?.message).isEqualTo("Esta reseña ya no existe")
    }

    // ---------- createReview ----------

    @Test
    fun `createReview sin nombre en el perfil usa el displayName y desnormaliza el vehiculo`() = runTest {
        // Arrange
        val sent = slot<NewReview>()
        every { authDataSource.getCurrentUser() } returns
            AuthUser(id = "uid-jose", email = "jose@example.com", displayName = "Jose Auth")
        coEvery { userDataSource.getUser("uid-jose") } returns UserDto(id = "uid-jose", fullName = "")
        coEvery { vehicleDataSource.getVehicleById("1") } returns porscheDto
        coEvery { remoteDataSource.createReview(capture(sent)) } answers {
            reviewDto(id = "nueva", user = ReviewUserSnapshot(sent.captured.authorName))
        }
        coEvery { remoteDataSource.getReviewsByVehicle("1") } returns emptyList()

        // Act
        val result = repository.createReview(
            vehicleId = "1",
            rating = 5,
            comment = "Brutal",
            location = ReviewLocation(lat = 4.6280, lng = -74.0643)
        )

        // Assert
        assertThat(result.isSuccess).isTrue()
        assertThat(sent.captured.userId).isEqualTo("uid-jose")
        assertThat(sent.captured.authorName).isEqualTo("Jose Auth")
        assertThat(sent.captured.vehicleBrand).isEqualTo("Porsche")
        assertThat(sent.captured.vehicleModel).isEqualTo("911 GT3")
        assertThat(sent.captured.rating).isEqualTo(5)
        assertThat(sent.captured.location).isEqualTo(ReviewLocation(lat = 4.6280, lng = -74.0643))
    }

    @Test
    fun `createReview con calificacion fuera de rango falla y no escribe`() = runTest {
        every { authDataSource.getCurrentUser() } returns AuthUser(id = "uid-jose", email = null, displayName = null)

        val result = repository.createReview(vehicleId = "1", rating = 7, comment = null, location = null)

        assertThat(result.exceptionOrNull()?.message).isEqualTo("La calificación debe ser un número entre 0 y 5")
        coVerify(exactly = 0) { remoteDataSource.createReview(any()) }
    }

    @Test
    fun `createReview notifica a quien ya habia reseniado y no al autor`() = runTest {
        every { authDataSource.getCurrentUser() } returns AuthUser(id = "uid-jose", email = null, displayName = "Jose")
        coEvery { userDataSource.getUser("uid-jose") } returns UserDto(id = "uid-jose", fullName = "Jose")
        coEvery { vehicleDataSource.getVehicleById("1") } returns porscheDto
        coEvery { remoteDataSource.createReview(any()) } returns reviewDto(id = "nueva")
        coEvery { remoteDataSource.getReviewsByVehicle("1") } returns listOf(
            reviewDto(id = "nueva", userId = "uid-jose"),
            reviewDto(id = "vieja", userId = "uid-otro")
        )

        repository.createReview(vehicleId = "1", rating = 5, comment = null, location = null)

        coVerify(exactly = 1) {
            notificationRepository.notify("uid-otro", NotificationType.REVIEW, "uid-jose", "1", "nueva")
        }
        coVerify(exactly = 0) { notificationRepository.notify("uid-jose", any(), any(), any(), any()) }
    }

    private fun reviewDto(
        id: String = "r1",
        userId: String = "uid-jose",
        createdAt: Long = 1_700_000_000_000L,
        likesCount: Int = 0,
        user: ReviewUserSnapshot? = ReviewUserSnapshot(fullName = "Jose Guardado", avatarUrl = "https://vieja")
    ) = ReviewFirestoreDto(
        id = id,
        rating = 4,
        comment = "Muy bueno",
        userId = userId,
        vehicleId = "1",
        createdAt = createdAt,
        updatedAt = createdAt,
        likesCount = likesCount,
        user = user,
        vehicle = ReviewVehicleSnapshot(brand = "Porsche", model = "911 GT3")
    )
}
