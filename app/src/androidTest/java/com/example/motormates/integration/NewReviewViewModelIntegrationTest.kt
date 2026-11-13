package com.example.motormates.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.repository.NotificationRepositoryImpl
import com.example.motormates.data.repository.ReviewRepositoryImpl
import com.example.motormates.data.repository.VehicleRepositoryImpl
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.source.FirebaseAuthDataSource
import com.example.motormates.data.source.FirestoreNotificationDataSource
import com.example.motormates.data.source.FirestoreReviewDataSource
import com.example.motormates.data.source.FirestoreReviewRealtimeDataSource
import com.example.motormates.data.source.FirestoreUserDataSource
import com.example.motormates.data.source.FirestoreVehicleDataSource
import com.example.motormates.data.source.PlaceNameDataSource
import com.example.motormates.ui.review.NewReviewUiState
import com.example.motormates.ui.review.NewReviewViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NewReviewViewModel con sus repositorios reales sobre el emulador de
 * Firebase. El ViewModel lanza sus corrutinas en viewModelScope y vuelve
 * de inmediato, así que cada prueba espera a que el estado cambie
 * (uiState.first { ... }) antes de verificar. Requiere npm run serve.
 */
@RunWith(AndroidJUnit4::class)
class NewReviewViewModelIntegrationTest {

    private val firestore = FirebaseEmulator.firestore
    private val userDataSource = FirestoreUserDataSource(firestore)
    private val vehicleDataSource = FirestoreVehicleDataSource(firestore)

    private val reviewRepository = ReviewRepositoryImpl(
        remoteDataSource = FirestoreReviewDataSource(firestore, FirebaseEmulator.storage),
        realtimeDataSource = FirestoreReviewRealtimeDataSource(firestore),
        vehicleDataSource = vehicleDataSource,
        userDataSource = userDataSource,
        authDataSource = FirebaseAuthDataSource(FirebaseEmulator.auth),
        notificationRepository = NotificationRepositoryImpl(
            FirestoreNotificationDataSource(firestore), userDataSource, vehicleDataSource
        )
    )

    private lateinit var uid: String
    private lateinit var viewModel: NewReviewViewModel

    @Before
    fun setUp() = runTest {
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
        FirebaseEmulator.seedVehicle(id = "1", brand = "Porsche", model = "911 GT3")
        uid = FirebaseEmulator.signInAs(EMAIL)
        userDataSource.createUser(UserDto(id = uid, fullName = "Jose Perfil", email = EMAIL, username = "jose"))
        // La ubicación se stubea a null en vez de usar la real: el emulador
        // no tiene GPS, y aquí lo que se prueba es que el ViewModel y
        // Firestore encajen, no de dónde sale la coordenada.
        viewModel = NewReviewViewModel(
            VehicleRepositoryImpl(vehicleDataSource),
            reviewRepository,
            NoLocationDataSource,
            NoPlaceNameDataSource
        )
    }

    @After
    fun tearDown() = runTest {
        FirebaseEmulator.auth.signOut()
        FirebaseEmulator.clearFirestore()
        FirebaseEmulator.clearAuth()
    }

    @Test
    fun startConVehiculoExistenteCargaSuResumenDesdeFirestore() = runTest {
        // Act
        viewModel.start(vehicleId = "1", reviewId = null)
        val state = awaitState { it.car != null || it.errorMessage != null }

        // Assert
        assertThat(state.errorMessage).isNull()
        assertThat(state.car?.title).isEqualTo("Porsche 911 GT3")
        assertThat(state.car?.year).isEqualTo(2024)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun saveConCalificacionValidaPublicaLaReseniaEnFirestore() = runTest {
        // Arrange
        viewModel.start(vehicleId = "1", reviewId = null)
        awaitState { it.car != null }
        viewModel.updateRating(5)
        viewModel.updateExperience("Brutal")

        // Act
        viewModel.save(vehicleId = "1")
        val state = awaitState { it.isSaved || it.errorMessage != null }

        // Assert: el estado y lo que quedó guardado en el emulador
        assertThat(state.errorMessage).isNull()
        assertThat(state.isSaved).isTrue()
        val saved = reviewRepository.getReviewsByUser(uid).getOrThrow().single()
        assertThat(saved.vehicleId).isEqualTo("1")
        assertThat(saved.rating).isEqualTo(5)
        assertThat(saved.comment).isEqualTo("Brutal")
        assertThat(saved.author?.fullName).isEqualTo("Jose Perfil")
    }

    /**
     * Espera (en tiempo real, no el virtual de runTest) hasta que el estado
     * cumpla la condición. Si nunca pasa, la prueba falla a los 10 s en vez
     * de colgarse.
     */
    private suspend fun awaitState(condition: (NewReviewUiState) -> Boolean): NewReviewUiState =
        withContext(Dispatchers.Default) {
            withTimeout(10_000) { viewModel.uiState.first(condition) }
        }

    private companion object {
        const val EMAIL = "jose@motormates.com"
    }
}

/**
 * Fuente de ubicación que nunca tiene posición, que es justo lo que pasa en
 * un emulador sin GPS configurado. Mantiene estas pruebas enfocadas en la
 * reseña y no en la ubicación.
 */
private object NoLocationDataSource : DeviceLocationDataSource {
    override suspend fun lastKnownLocation(): ReviewLocation? = null
}

/**
 * El Geocoder necesita red y servicio de Google; estas pruebas corren contra
 * el emulador de Firebase y no deberían depender de eso.
 */
private object NoPlaceNameDataSource : PlaceNameDataSource {
    override suspend fun placeNameFor(location: ReviewLocation): String? = null
}
