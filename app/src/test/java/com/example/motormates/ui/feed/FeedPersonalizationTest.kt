package com.example.motormates.ui.feed

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.LikeRepository
import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.StoryRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.repository.news.NewsRepository
import com.example.motormates.data.seed.FirestoreSeeder
import com.example.motormates.data.util.OnboardingTestData
import com.example.motormates.data.util.OnboardingTestData.review
import com.example.motormates.data.util.OnboardingTestData.vehicle
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Lo que agrega el onboarding al feed: orden de Explorar, "Para ti",
 * sugerencias y reseñas destacadas en Siguiendo.
 */
class FeedPersonalizationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicleRepository: VehicleRepository = mockk()
    private val reviewRepository: ReviewRepository = mockk()
    private val followRepository: FollowRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val storyRepository: StoryRepository = mockk()
    private val seeder: FirestoreSeeder = mockk(relaxed = true)
    private val preferencesRepository: PreferencesRepository = mockk()
    private val timeSource: TimeSource = mockk()
    private val newsRepository: NewsRepository = mockk()

    private val preferences = MutableStateFlow(UserPreferences(categories = listOf("SUV")))
    private val following = MutableStateFlow<List<String>>(emptyList())

    @Before
    fun setUp() {
        every { authRepository.getCurrentUser() } returns AuthUser(id = "yo", email = null, displayName = null)
        every { timeSource.nowMillis() } returns OnboardingTestData.NOW
        coEvery { vehicleRepository.getVehicles() } returns Result.success(
            listOf(vehicle("1", "Porsche", "Deportivo"), vehicle("2", "Toyota", "SUV"))
        )
        every { reviewRepository.observeRecentReviews(any()) } returns flowOf(
            Result.success(
                listOf(
                    review("deportivo", vehicleId = "1", userId = "ana", rating = 5, likes = 30),
                    review("suv", vehicleId = "2", userId = "beto", rating = 3)
                )
            )
        )
        every { reviewRepository.observeReviewsByUsers(any()) } returns flowOf(Result.success(emptyList()))
        every { followRepository.observeFollowingIds("yo") } returns following
        coEvery { followRepository.followUser("yo", any()) } returns Result.success(Unit)
        every { likeRepository.observeLikedReviewIds(any(), any()) } returns flowOf(emptySet())
        every { storyRepository.observeActiveStories() } returns flowOf(emptyList())
        every { preferencesRepository.observeMine() } returns preferences
        coEvery { newsRepository.getLatestNews() } returns Result.success(emptyList())
    }

    private fun newViewModel() = FeedViewModel(
        vehicleRepository, reviewRepository, followRepository, authRepository, likeRepository,
        storyRepository, seeder, preferencesRepository, timeSource, newsRepository
    )

    @Test
    fun `explorar pone primero lo que le gusta, con su motivo, y arma Para ti`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        // El Porsche es más popular, pero el usuario dijo que le gustan los SUV.
        assertThat(state.vehicles.map { it.id }).containsExactly("2", "1").inOrder()
        assertThat(state.vehicles[0].reason).isEqualTo("Porque te gustan los SUV")
        assertThat(state.vehicles[1].reason).isNull()
        assertThat(state.forYou.map { it.id }).containsExactly("2")
        assertThat(state.hasPreferences).isTrue()
    }

    @Test
    fun `al editar las preferencias el feed se reordena solo`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        preferences.value = UserPreferences(categories = listOf("Deportivo"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.vehicles.first().id).isEqualTo("1")
        assertThat(vm.uiState.value.forYou.map { it.id }).containsExactly("1")
    }

    @Test
    fun `sin gustos no hay Para ti y se ofrece personalizar`() = runTest {
        preferences.value = UserPreferences(skipped = true)
        val vm = newViewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.forYou).isEmpty()
        assertThat(vm.uiState.value.hasPreferences).isFalse()
    }

    @Test
    fun `siguiendo vacio se completa con sugerencias y destacadas`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.showFollowingDiscovery).isTrue()
        assertThat(state.suggestions.map { it.userId }).containsExactly("beto", "ana").inOrder()
        assertThat(state.featuredReviews.map { it.id }.first()).isEqualTo("suv")
    }

    @Test
    fun `seguir una sugerencia la saca de la lista de una vez`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.followSuggested("beto")
        advanceUntilIdle()

        coVerify { followRepository.followUser("yo", "beto") }
        assertThat(vm.uiState.value.suggestions.map { it.userId }).containsExactly("ana")
        // Sus reseñas tampoco salen como destacadas: ya las verá como seguido.
        assertThat(vm.uiState.value.featuredReviews.map { it.id }).doesNotContain("suv")
    }
}
