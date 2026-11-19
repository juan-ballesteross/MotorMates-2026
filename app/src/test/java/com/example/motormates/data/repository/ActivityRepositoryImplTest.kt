package com.example.motormates.data.repository

import com.example.motormates.data.model.ActivityType
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.FollowActivity
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewVehicleRef
import com.example.motormates.data.source.ActivityDataSource
import com.example.motormates.data.source.AuthDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Cada fuente de la actividad es opcional: una que falle no tumba la pestaña. */
class ActivityRepositoryImplTest {

    private val activityDataSource = mockk<ActivityDataSource>()
    private val reviewRepository = mockk<ReviewRepository>()
    private val vehicleRepository = mockk<VehicleRepository>()
    private val authDataSource = mockk<AuthDataSource>()

    private val repository = ActivityRepositoryImpl(activityDataSource, reviewRepository, vehicleRepository, authDataSource)

    private val myReview = Review(
        id = "r1", rating = 5, comment = "ok", userId = "me", vehicleId = "p1",
        createdAt = "2026-11-16T10:00:00Z", author = null, vehicle = ReviewVehicleRef("p1", "Porsche", "911 GT3")
    )

    @Before
    fun setUp() {
        every { authDataSource.getCurrentUser() } returns AuthUser(id = "me", email = null, displayName = null)
        coEvery { reviewRepository.getReviewsByUser("me") } returns Result.success(listOf(myReview))
        coEvery { vehicleRepository.getVehicles() } returns Result.success(emptyList())
        coEvery { activityDataSource.getReviewLikesBy("me", any()) } returns emptyList()
        coEvery { activityDataSource.getFollowingBy("me") } returns
            listOf(FollowActivity("ivan", "Iván", 1_763_300_000_000L))
        coEvery { activityDataSource.getStoriesBy("me") } returns emptyList()
    }

    @Test
    fun `junta las fuentes en una sola linea de tiempo`() = runTest {
        val timeline = repository.getMyActivity().getOrThrow()

        assertThat(timeline.map { it.type }).containsExactly(ActivityType.REVIEW, ActivityType.FOLLOW)
    }

    @Test
    fun `si los likes fallan por falta del indice se muestra lo demas`() = runTest {
        coEvery { activityDataSource.getReviewLikesBy(any(), any()) } throws RuntimeException("FAILED_PRECONDITION: requires an index")

        val result = repository.getMyActivity()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().map { it.type }).containsExactly(ActivityType.REVIEW, ActivityType.FOLLOW)
    }

    @Test
    fun `solo es error si no se pudo leer ninguna fuente`() = runTest {
        coEvery { reviewRepository.getReviewsByUser(any()) } returns Result.failure(Exception("x"))
        coEvery { vehicleRepository.getVehicles() } returns Result.failure(Exception("x"))
        coEvery { activityDataSource.getReviewLikesBy(any(), any()) } throws RuntimeException("x")
        coEvery { activityDataSource.getFollowingBy(any()) } throws RuntimeException("x")
        coEvery { activityDataSource.getStoriesBy(any()) } throws RuntimeException("x")

        val result = repository.getMyActivity()

        assertThat(result.exceptionOrNull()?.message).isEqualTo("No se pudo cargar tu actividad")
    }

    @Test
    fun `sin sesion no consulta nada`() = runTest {
        every { authDataSource.getCurrentUser() } returns null

        assertThat(repository.getMyActivity().isFailure).isTrue()
    }
}
