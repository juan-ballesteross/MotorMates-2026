package com.example.motormates.data.repository

import com.example.motormates.data.dto.ReviewDtoGeneric
import com.example.motormates.data.dto.ReviewFirestoreDto
import com.example.motormates.data.dto.ReviewUserSnapshot
import com.example.motormates.data.dto.ReviewVehicleSnapshot
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.dto.VehicleDtoGeneric
import com.example.motormates.data.dto.VehicleFirestoreDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.NewReview
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.data.model.toReviewUi
import com.example.motormates.data.remote.datasource.ReviewRemoteDataSource
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRepositoryImplTest {

    private val porsche = VehicleFirestoreDto(id = "1", brand = "Porsche", model = "911 GT3", year = 2024)
    private val jose = AuthUser(id = "uid-jose", email = "jose@example.com", displayName = "Jose Auth")

    @Test
    fun createReviewGuardaAutorYVehiculoDesnormalizados() = runTest {
        val reviews = FakeReviewDataSource()
        val repository = repository(
            reviews = reviews,
            currentUser = jose,
            users = listOf(UserDto(id = jose.id, fullName = "Jose Perfil", avatarUrl = "https://foto"))
        )

        val created = repository.createReview(vehicleId = "1", rating = 5, comment = "Brutal").getOrThrow()

        val saved = reviews.created.single()
        assertEquals("uid-jose", saved.userId)
        assertEquals("Jose Perfil", saved.authorName)
        assertEquals("https://foto", saved.authorAvatarUrl)
        assertEquals("Porsche", saved.vehicleBrand)
        assertEquals("911 GT3", saved.vehicleModel)
        assertEquals("uid-jose", created.userId)
        assertEquals("Jose Perfil", created.author?.fullName)
    }

    @Test
    fun createReviewSinSesionFallaYNoEscribe() = runTest {
        val reviews = FakeReviewDataSource()
        val repository = repository(reviews = reviews, currentUser = null)

        val result = repository.createReview(vehicleId = "1", rating = 4, comment = null)

        assertTrue(result.isFailure)
        assertTrue(reviews.created.isEmpty())
    }

    @Test
    fun createReviewEnVehiculoInexistenteDevuelveMensajeEnEspanol() = runTest {
        val repository = repository(reviews = FakeReviewDataSource(), currentUser = jose)

        val result = repository.createReview(vehicleId = "999", rating = 4, comment = null)

        assertEquals("El usuario o el vehículo ya no existe", result.exceptionOrNull()?.message)
    }

    @Test
    fun vehiculoInexistenteDevuelveMensajeDeNoEncontrado() = runTest {
        val repository = VehicleRepositoryImpl(FakeVehicleDataSource(listOf(porsche)))

        val result = repository.getVehicleById("999")

        assertEquals("Este vehículo no existe o fue eliminado", result.exceptionOrNull()?.message)
    }

    @Test
    fun reviewDeFirestoreSeMapeaAlDominioYALaUi() {
        val dto = ReviewFirestoreDto(
            id = "r1",
            rating = 4,
            comment = "Muy bueno",
            userId = "uid-jose",
            vehicleId = "1",
            createdAt = 1_700_000_000_000L,
            user = ReviewUserSnapshot(fullName = "Jose", avatarUrl = "https://foto"),
            vehicle = ReviewVehicleSnapshot(brand = "Porsche", model = "911 GT3")
        )

        val review = dto.toDomain()

        assertEquals("2023-11-14T22:13:20Z", review.createdAt)
        assertEquals("Porsche", review.vehicle?.brand)
        assertTrue(review.toReviewUi(currentUserId = "uid-jose").isMine)
        assertFalse(review.toReviewUi(currentUserId = "otro").isMine)
        assertFalse(review.toReviewUi(currentUserId = null).isMine)
        assertEquals("https://foto", review.toReviewUi(null).avatarUrl)
    }

    @Test
    fun detalleCalculaPromedioYCeroSinResenas() {
        val vehicle = porsche.toDomain()
        val reviews = listOf(4, 5).mapIndexed { i, rating ->
            ReviewFirestoreDto(id = "r$i", rating = rating, userId = "u", vehicleId = "1").toDomain()
        }

        assertEquals(4.5f, vehicle.toCarDetailUi(reviews).calificacion, 0.001f)
        assertEquals(0f, vehicle.toCarDetailUi(emptyList()).calificacion, 0.001f)
        assertNull(ReviewFirestoreDto().toDomain().createdAt)
    }

    private fun repository(
        reviews: FakeReviewDataSource,
        currentUser: AuthUser?,
        users: List<UserDto> = emptyList()
    ) = ReviewRepositoryImpl(
        remoteDataSource = reviews,
        vehicleDataSource = FakeVehicleDataSource(listOf(porsche)),
        userDataSource = FakeUserDataSource(users),
        authDataSource = FakeAuthDataSource(currentUser = currentUser)
    )
}

private class FakeVehicleDataSource(
    private val vehicles: List<VehicleFirestoreDto>
) : VehicleRemoteDataSource {

    override suspend fun getVehicles(): List<VehicleDtoGeneric> = vehicles

    override suspend fun getVehicleById(id: String): VehicleDtoGeneric =
        vehicles.firstOrNull { it.id == id } ?: throw NoSuchElementException(id)
}

private class FakeReviewDataSource : ReviewRemoteDataSource {

    val created = mutableListOf<NewReview>()

    override suspend fun getReviewsByVehicle(vehicleId: String): List<ReviewDtoGeneric> = emptyList()

    override suspend fun getReviewsByUser(userId: String): List<ReviewDtoGeneric> = emptyList()

    override suspend fun createReview(review: NewReview): ReviewDtoGeneric {
        created += review
        return ReviewFirestoreDto(
            id = "nueva",
            rating = review.rating,
            comment = review.comment,
            userId = review.userId,
            vehicleId = review.vehicleId,
            user = ReviewUserSnapshot(review.authorName, review.authorAvatarUrl),
            vehicle = ReviewVehicleSnapshot(review.vehicleBrand, review.vehicleModel)
        )
    }

    override suspend fun updateReview(id: String, rating: Int?, comment: String?): ReviewDtoGeneric =
        throw NoSuchElementException(id)

    override suspend fun deleteReview(id: String) = Unit
}
