package com.example.motormates.ui.onboarding

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.CarRef
import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.ReviewLocation
import com.example.motormates.data.model.User
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.CarCatalogRepository
import com.example.motormates.data.repository.CarModelOption
import com.example.motormates.data.repository.CarModelSource
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.GarageRepository
import com.example.motormates.data.repository.PreferencesRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.UserRepository
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.source.DeviceLocationDataSource
import com.example.motormates.data.util.OnboardingTestData
import com.example.motormates.data.util.OnboardingTestData.review
import com.example.motormates.data.util.OnboardingTestData.vehicle
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository: PreferencesRepository = mockk()
    private val vehicleRepository: VehicleRepository = mockk()
    private val reviewRepository: ReviewRepository = mockk()
    private val followRepository: FollowRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val carCatalogRepository: CarCatalogRepository = mockk()
    private val garageRepository: GarageRepository = mockk()
    private val deviceLocation: DeviceLocationDataSource = mockk()
    private val timeSource: TimeSource = mockk()

    private val vehicles = listOf(
        vehicle("1", "Porsche", "Deportivo"),
        vehicle("2", "Toyota", "SUV"),
        vehicle("3", "Mazda", "SUV", ownerId = "yo", model = "3")
    )

    @Before
    fun setUp() {
        every { authRepository.getCurrentUser() } returns AuthUser(id = "yo", email = null, displayName = null)
        every { timeSource.nowMillis() } returns OnboardingTestData.NOW
        coEvery { preferencesRepository.getMine() } returns Result.success(UserPreferences())
        coEvery { preferencesRepository.save(any()) } returns Result.success(Unit)
        coEvery { preferencesRepository.skip() } returns Result.success(Unit)
        coEvery { vehicleRepository.getVehicles() } returns Result.success(vehicles)
        coEvery { carCatalogRepository.brands() } returns listOf("BYD", "Mazda", "Renault", "Toyota")
        coEvery { carCatalogRepository.models(any(), any()) } returns listOf(
            CarModelOption("Duster", CarModelSource.LOCAL),
            CarModelOption("Sandero", CarModelSource.LOCAL)
        )
        coEvery { userRepository.getCurrentUserProfile("yo") } returns Result.success(
            User("yo", "Yo", "yo@x.com", "yo", "Medellín, Colombia", "", null, null, 0, 0)
        )
        every { reviewRepository.observeRecentReviews(any()) } returns flowOf(
            Result.success(
                listOf(
                    review("a", vehicleId = "2", userId = "fan-suv"),
                    review("b", vehicleId = "1", userId = "seguido")
                )
            )
        )
        every { followRepository.observeFollowingIds("yo") } returns flowOf(listOf("seguido"))
        coEvery { followRepository.followUser(any(), any()) } returns Result.success(Unit)
        coEvery { followRepository.unfollowUser(any(), any()) } returns Result.success(Unit)
    }

    private fun newViewModel() = OnboardingViewModel(
        preferencesRepository, vehicleRepository, reviewRepository, followRepository, authRepository,
        userRepository, carCatalogRepository, garageRepository, deviceLocation, timeSource
    )

    private fun OnboardingViewModel.goTo(step: OnboardingStep) {
        while (state.value.step != step) next()
    }

    @Test
    fun `si ya contesto antes termina de una vez sin cargar nada`() = runTest {
        coEvery { preferencesRepository.getMine() } returns Result.success(UserPreferences(skipped = true))
        val vm = newViewModel()

        vm.load(editMode = false)
        advanceUntilIdle()

        assertThat(vm.state.value.isFinished).isTrue()
        coVerify(exactly = 0) { vehicleRepository.getVehicles() }
    }

    @Test
    fun `si no se pueden leer las preferencias deja entrar al feed`() = runTest {
        coEvery { preferencesRepository.getMine() } returns Result.failure(Exception("sin red"))
        val vm = newViewModel()

        vm.load(editMode = false)
        advanceUntilIdle()

        assertThat(vm.state.value.isFinished).isTrue()
    }

    @Test
    fun `usuario nuevo ve el primer paso con fotos, marcas y su ciudad del perfil`() = runTest {
        val vm = newViewModel()

        vm.load(editMode = false)
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isFinished).isFalse()
        assertThat(state.step).isEqualTo(OnboardingStep.CATEGORIES)
        assertThat(state.categoryImages["SUV"]).isEqualTo("https://example.com/2.jpg")
        assertThat(state.categoryImages["Pickup"]).isNull()
        // Las marcas del catálogo de la app primero, luego las de la lista local.
        assertThat(state.brandOptions).containsExactly("Mazda", "Porsche", "Toyota", "BYD", "Renault").inOrder()
        assertThat(state.city).isEqualTo("Medellín")
    }

    @Test
    fun `marcar y desmarcar respeta el tope de opciones`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        (1..12).forEach { vm.toggleBrand("Marca $it") }
        vm.toggleCategory("SUV")
        vm.toggleCategory("SUV")

        assertThat(vm.state.value.brands).hasSize(10)
        assertThat(vm.state.value.categories).isEmpty()
    }

    @Test
    fun `el carro a medias no deja continuar`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()
        vm.goTo(OnboardingStep.CAR)

        vm.setHasCar(true)
        vm.updateCarBrand("Renault")
        vm.updateCarModel("Duster")
        vm.updateCarYear("20a2")

        assertThat(vm.state.value.carYear).isEqualTo("202")
        assertThat(vm.state.value.canContinue).isFalse()
        vm.next()
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.CAR)

        vm.updateCarYear("2022")
        vm.selectCarCategory("SUV")
        assertThat(vm.state.value.canContinue).isTrue()
    }

    @Test
    fun `sin carro se puede seguir y atras vuelve al paso anterior`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()
        vm.goTo(OnboardingStep.CAR)

        vm.setHasCar(false)
        vm.next()
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.INTERESTS)

        vm.back()
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.CAR)
    }

    @Test
    fun `al escribir la marca del carro se buscan sus modelos`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        vm.updateCarBrand("Renault")
        advanceUntilIdle()

        assertThat(vm.state.value.carModelOptions.map { it.name }).containsExactly("Duster", "Sandero")
        vm.updateCarModel("dus")
        assertThat(vm.state.value.visibleCarModels.map { it.name }).containsExactly("Duster")
    }

    @Test
    fun `el ultimo paso sugiere personas segun los gustos y sin los ya seguidos`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()
        vm.toggleCategory("SUV")

        vm.goTo(OnboardingStep.PEOPLE)
        advanceUntilIdle()

        assertThat(vm.state.value.isLastStep).isTrue()
        assertThat(vm.state.value.suggestions.map { it.userId }).containsExactly("fan-suv")
    }

    @Test
    fun `seguir es optimista y se revierte si falla`() = runTest {
        coEvery { followRepository.followUser("yo", "falla") } returns Result.failure(Exception("Sin red"))
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        vm.toggleFollow("fan-suv")
        assertThat(vm.state.value.followed).contains("fan-suv")
        vm.toggleFollow("falla")
        advanceUntilIdle()

        assertThat(vm.state.value.followed).containsExactly("fan-suv")
        assertThat(vm.state.value.errorMessage).isEqualTo("Sin red")
    }

    @Test
    fun `terminar guarda las respuestas y agrega el carro al garaje`() = runTest {
        val saved = slot<UserPreferences>()
        val draft = slot<VehicleDraft>()
        coEvery { preferencesRepository.save(capture(saved)) } returns Result.success(Unit)
        coEvery { garageRepository.publishVehicle(capture(draft), null) } returns Result.success(vehicles[0])
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        vm.toggleCategory("SUV")
        vm.toggleInterest(Interest.MODIFY)
        vm.setHasCar(true)
        vm.updateCarBrand("Renault")
        vm.updateCarModel("Duster")
        vm.updateCarYear("2022")
        vm.finish()
        advanceUntilIdle()

        assertThat(saved.captured.categories).containsExactly("SUV")
        assertThat(saved.captured.interests).containsExactly(Interest.MODIFY)
        assertThat(saved.captured.city).isEqualTo("Medellín")
        assertThat(saved.captured.car).isEqualTo(CarRef("Renault", "Duster", 2022))
        // La categoría del carro se propone con la primera favorita.
        assertThat(draft.captured.category).isEqualTo("SUV")
        assertThat(vm.state.value.isFinished).isTrue()
    }

    @Test
    fun `un carro que ya esta en el garaje no se vuelve a crear`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        vm.setHasCar(true)
        vm.updateCarBrand("mazda")
        vm.updateCarModel("3")
        vm.updateCarYear("2024")
        vm.selectCarCategory("SUV")
        assertThat(vm.state.value.carAlreadyInGarage).isTrue()

        vm.finish()
        advanceUntilIdle()

        coVerify(exactly = 0) { garageRepository.publishVehicle(any(), any()) }
        assertThat(vm.state.value.isFinished).isTrue()
    }

    @Test
    fun `si falla guardar se muestra el error y no termina`() = runTest {
        coEvery { preferencesRepository.save(any()) } returns Result.failure(Exception("No se pudieron guardar tus preferencias"))
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        vm.finish()
        advanceUntilIdle()

        assertThat(vm.state.value.isFinished).isFalse()
        assertThat(vm.state.value.errorMessage).isEqualTo("No se pudieron guardar tus preferencias")
    }

    @Test
    fun `si falla agregar el carro igual termina despues del aviso`() = runTest {
        coEvery { garageRepository.publishVehicle(any(), any()) } returns Result.failure(Exception("x"))
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()
        vm.setHasCar(true)
        vm.updateCarBrand("Renault")
        vm.updateCarModel("Duster")
        vm.updateCarYear("2022")
        vm.selectCarCategory("SUV")

        vm.finish()
        advanceUntilIdle()

        assertThat(vm.state.value.isFinished).isTrue()
        coVerify { preferencesRepository.save(any()) }
    }

    @Test
    fun `saltar avisa al repositorio y termina`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        vm.skip()
        advanceUntilIdle()

        coVerify { preferencesRepository.skip() }
        assertThat(vm.state.value.isFinished).isTrue()
    }

    @Test
    fun `al editar se prellena todo, no hay paso de personas y el ultimo guarda`() = runTest {
        coEvery { preferencesRepository.getMine() } returns Result.success(
            UserPreferences(
                categories = listOf("Clásico"),
                brands = listOf("Porsche"),
                interests = listOf(Interest.PHOTO),
                city = "Cali",
                car = CarRef("Renault", "4", 1985),
                completed = true
            )
        )
        val vm = newViewModel()

        vm.load(editMode = true)
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.isFinished).isFalse()
        assertThat(state.steps).doesNotContain(OnboardingStep.PEOPLE)
        assertThat(state.categories).containsExactly("Clásico")
        assertThat(state.city).isEqualTo("Cali")
        assertThat(state.hasCar).isTrue()
        assertThat(state.carYear).isEqualTo("1985")

        vm.goTo(OnboardingStep.CITY)
        assertThat(vm.state.value.isLastStep).isTrue()
        vm.next()
        advanceUntilIdle()
        coVerify { preferencesRepository.save(match { it.city == "Cali" }) }
        // El carro ya estaba declarado: no se publica otra vez.
        coVerify(exactly = 0) { garageRepository.publishVehicle(any(), any()) }
        assertThat(vm.state.value.isFinished).isTrue()
    }

    @Test
    fun `la ubicacion elige la ciudad mas cercana o avisa si no hay`() = runTest {
        val vm = newViewModel()
        vm.load(editMode = false)
        advanceUntilIdle()

        coEvery { deviceLocation.lastKnownLocation() } returns ReviewLocation(3.45, -76.53)
        vm.detectCity()
        advanceUntilIdle()
        assertThat(vm.state.value.city).isEqualTo("Cali")

        coEvery { deviceLocation.lastKnownLocation() } returns ReviewLocation(4.14, -73.63)
        vm.detectCity()
        advanceUntilIdle()
        assertThat(vm.state.value.city).isEqualTo("Cali")
        assertThat(vm.state.value.errorMessage).contains("elige la más cercana")
    }
}
