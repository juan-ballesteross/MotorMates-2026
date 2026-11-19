package com.example.motormates.ui.compare

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.CompareSide
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.SpecWinner
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.ReviewRepository
import com.example.motormates.data.repository.VehicleRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * CompareViewModel con los repositorios mockeados: el flujo de entrar con un
 * carro, elegir el segundo, intercambiar lados y los errores.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var followRepository: FollowRepository
    private lateinit var viewModel: CompareViewModel

    private val mustang = vehicle("mustang", "Ford", "Mustang GT", "502 hp", "3.4 s", "318 km/h")
    private val wrangler = vehicle("wrangler", "Jeep", "Wrangler", "285 hp", "6.9 s", "180 km/h")
    private val tesla = vehicle("tesla", "Tesla", "Model 3", "283 hp", "6.1 s", "201 km/h")

    @Before
    fun setUp() {
        vehicleRepository = mockk()
        reviewRepository = mockk()
        authRepository = mockk()
        followRepository = mockk()
        // Sigo a Sofía y a Iván, no a Rodrigo.
        every { followRepository.observeFollowingIds("me") } returns flowOf(listOf("sofia", "ivan"))
        every { authRepository.getCurrentUser() } returns AuthUser(id = "me", email = null, displayName = null)
        coEvery { reviewRepository.getReviewsByUser("me") } returns Result.success(emptyList())

        coEvery { vehicleRepository.getVehicles() } returns Result.success(listOf(wrangler, mustang, tesla))
        listOf(mustang, wrangler, tesla).forEach { v ->
            coEvery { vehicleRepository.getVehicleById(v.id) } returns Result.success(v)
            coEvery { reviewRepository.getReviewsByVehicle(v.id) } returns Result.success(emptyList())
        }

        viewModel = CompareViewModel(vehicleRepository, reviewRepository, authRepository, followRepository)
    }

    // ---------- "Comparar con el mío" ----------

    @Test
    fun `con un solo carro propio compara directo sin abrir el selector`() = runTest {
        coEvery { vehicleRepository.getVehicles() } returns
            Result.success(listOf(wrangler, mustang.copy(ownerId = "me"), tesla))

        viewModel.start("wrangler", null, compareWithMine = true)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.picker.isOpen).isFalse()
        assertThat(state.isFindingMyCar).isFalse()
        assertThat(state.rightId).isEqualTo("mustang")
        assertThat(state.comparison?.right?.modelo).isEqualTo("Mustang GT")
    }

    @Test
    fun `los carros resenados por mi tambien cuentan como mios`() = runTest {
        coEvery { reviewRepository.getReviewsByUser("me") } returns
            Result.success(listOf(review("r1", "tesla", 5, likes = 0, comment = "Mi carro")))

        viewModel.start("wrangler", null, compareWithMine = true)
        advanceUntilIdle()

        assertThat(viewModel.state.value.rightId).isEqualTo("tesla")
    }

    @Test
    fun `con varios carros mios abre el selector con ellos arriba`() = runTest {
        coEvery { vehicleRepository.getVehicles() } returns
            Result.success(listOf(wrangler, mustang.copy(ownerId = "me"), tesla))
        coEvery { reviewRepository.getReviewsByUser("me") } returns
            Result.success(listOf(review("r1", "tesla", 4, likes = 0, comment = "ok")))

        viewModel.start("wrangler", null, compareWithMine = true)
        advanceUntilIdle()

        val picker = viewModel.state.value.picker
        assertThat(picker.isOpen).isTrue()
        // Primero el que tengo a mi nombre, después el reseñado.
        assertThat(picker.myVisibleVehicles.map { it.id }).containsExactly("mustang", "tesla").inOrder()
        assertThat(picker.otherVisibleVehicles).isEmpty()
        assertThat(picker.notice).isNotNull()
    }

    @Test
    fun `sin carros mios abre el selector normal con un aviso`() = runTest {
        viewModel.start("wrangler", null, compareWithMine = true)
        advanceUntilIdle()

        val picker = viewModel.state.value.picker
        assertThat(picker.isOpen).isTrue()
        assertThat(picker.myVisibleVehicles).isEmpty()
        assertThat(picker.otherVisibleVehicles.map { it.id }).containsExactly("mustang", "tesla")
        assertThat(picker.notice).contains("Todavía no tienes carros")
    }

    @Test
    fun `el carro del perfil no cuenta como mio aunque lo haya resenado`() = runTest {
        coEvery { reviewRepository.getReviewsByUser("me") } returns
            Result.success(listOf(review("r1", "wrangler", 5, likes = 0, comment = "ok")))

        viewModel.start("wrangler", null, compareWithMine = true)
        advanceUntilIdle()

        assertThat(viewModel.state.value.picker.myVehicleIds).isEmpty()
    }

    @Test
    fun `entrando con un solo carro lo muestra y abre el selector para el segundo`() = runTest {
        viewModel.start("mustang", null)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.left?.modelo).isEqualTo("Mustang GT")
        assertThat(state.right).isNull()
        assertThat(state.comparison).isNull()
        assertThat(state.picker.target).isEqualTo(CompareSide.RIGHT)
    }

    @Test
    fun `el selector no ofrece el carro que ya esta en la comparacion`() = runTest {
        viewModel.start("mustang", null)
        advanceUntilIdle()

        val ids = viewModel.state.value.picker.visibleVehicles.map { it.id }
        assertThat(ids).containsExactly("wrangler", "tesla")
    }

    @Test
    fun `el selector filtra por marca o modelo sin importar mayusculas`() = runTest {
        viewModel.start("mustang", null)
        advanceUntilIdle()

        viewModel.onPickerQueryChange("TESLA")

        assertThat(viewModel.state.value.picker.visibleVehicles.map { it.id }).containsExactly("tesla")
    }

    @Test
    fun `al elegir el segundo carro se cierra el selector y aparece la comparacion`() = runTest {
        viewModel.start("mustang", null)
        advanceUntilIdle()

        viewModel.selectVehicle("wrangler")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.picker.isOpen).isFalse()
        assertThat(state.rightId).isEqualTo("wrangler")
        assertThat(state.comparison).isNotNull()
        assertThat(state.comparison?.overallWinner).isEqualTo(SpecWinner.LEFT)
    }

    @Test
    fun `entrando con los dos ids compara de una sin abrir el selector`() = runTest {
        viewModel.start("mustang", "wrangler")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.picker.isOpen).isFalse()
        assertThat(state.comparison?.right?.modelo).isEqualTo("Wrangler")
    }

    @Test
    fun `intercambiar lados no vuelve a consultar el repositorio`() = runTest {
        viewModel.start("mustang", "wrangler")
        advanceUntilIdle()

        viewModel.swapSides()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.leftId).isEqualTo("wrangler")
        assertThat(state.left?.modelo).isEqualTo("Wrangler")
        assertThat(state.comparison?.overallWinner).isEqualTo(SpecWinner.RIGHT)
        coVerify(exactly = 1) { vehicleRepository.getVehicleById("mustang") }
        coVerify(exactly = 1) { vehicleRepository.getVehicleById("wrangler") }
    }

    @Test
    fun `cambiar el carro de la izquierda recalcula la comparacion`() = runTest {
        viewModel.start("mustang", "wrangler")
        advanceUntilIdle()

        viewModel.openPicker(CompareSide.LEFT)
        viewModel.selectVehicle("tesla")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.leftId).isEqualTo("tesla")
        assertThat(state.comparison?.left?.modelo).isEqualTo("Model 3")
    }

    @Test
    fun `la segunda llamada a start se ignora para no perder la eleccion al rotar`() = runTest {
        viewModel.start("mustang", null)
        advanceUntilIdle()
        viewModel.selectVehicle("wrangler")
        advanceUntilIdle()

        viewModel.start("mustang", null)
        advanceUntilIdle()

        assertThat(viewModel.state.value.rightId).isEqualTo("wrangler")
    }

    @Test
    fun `si falla la carga del vehiculo muestra el error y reintentar lo recupera`() = runTest {
        coEvery { vehicleRepository.getVehicleById("mustang") } returns
            Result.failure(Exception("Sin conexión")) andThen Result.success(mustang)

        viewModel.start("mustang", "wrangler")
        advanceUntilIdle()
        assertThat(viewModel.state.value.errorMessage).isEqualTo("Sin conexión")
        assertThat(viewModel.state.value.comparison).isNull()

        viewModel.retry()
        advanceUntilIdle()
        assertThat(viewModel.state.value.errorMessage).isNull()
        assertThat(viewModel.state.value.comparison).isNotNull()
    }

    @Test
    fun `las resenas alimentan la seccion de comunidad`() = runTest {
        coEvery { reviewRepository.getReviewsByVehicle("wrangler") } returns Result.success(
            listOf(review("1", "wrangler", 5, likes = 4, comment = "Para la trocha no hay otro"))
        )

        viewModel.start("mustang", "wrangler")
        advanceUntilIdle()

        val right = viewModel.state.value.comparison?.right
        assertThat(right?.numeroResenas).isEqualTo(1)
        assertThat(right?.topReview?.comentario).isEqualTo("Para la trocha no hay otro")
    }

    @Test
    fun `si falla la lista del selector muestra el error ahi`() = runTest {
        coEvery { vehicleRepository.getVehicles() } returns Result.failure(Exception("No se pudieron cargar los vehículos"))

        viewModel.start("mustang", null)
        advanceUntilIdle()

        val picker = viewModel.state.value.picker
        assertThat(picker.isLoading).isFalse()
        assertThat(picker.errorMessage).isEqualTo("No se pudieron cargar los vehículos")
    }

    // ---------- Selector en dos pasos: persona -> carro ----------

    private fun withOwners() {
        coEvery { vehicleRepository.getVehicles() } returns Result.success(
            listOf(
                mustang.copy(ownerId = "me", ownerName = "Diego"),
                wrangler.copy(ownerId = "sofia", ownerName = "Sofía"),
                tesla.copy(ownerId = "sofia", ownerName = "Sofía"),
                vehicle("ranger", "Ford", "Ranger", "210 hp", "10.3 s", "180 km/h").copy(ownerId = "ivan", ownerName = "Iván")
            )
        )
        listOf("ranger").forEach { id ->
            coEvery { vehicleRepository.getVehicleById(id) } returns
                Result.success(vehicle("ranger", "Ford", "Ranger", "210 hp", "10.3 s", "180 km/h"))
            coEvery { reviewRepository.getReviewsByVehicle(id) } returns Result.success(emptyList())
        }
    }

    @Test
    fun `primero se elige la persona y se ve cuantos carros tiene`() = runTest {
        withOwners()
        viewModel.start("tesla", null)
        advanceUntilIdle()

        val picker = viewModel.state.value.picker
        assertThat(picker.selectedOwnerKey).isNull()
        // Mis carros van en "Tus carros"; el Tesla ya está a la izquierda y no cuenta.
        assertThat(picker.owners.map { it.name to it.carCount })
            .containsExactly("Tus carros" to 1, "Iván" to 1, "Sofía" to 1).inOrder()
    }

    @Test
    fun `solo salen las personas que sigo`() = runTest {
        coEvery { vehicleRepository.getVehicles() } returns Result.success(
            listOf(
                wrangler.copy(ownerId = "sofia", ownerName = "Sofía"),
                tesla.copy(ownerId = "rodrigo", ownerName = "Rodrigo")
            )
        )
        viewModel.start("mustang", null)
        advanceUntilIdle()

        assertThat(viewModel.state.value.picker.owners.map { it.name }).containsExactly("Sofía")
    }

    @Test
    fun `al elegir una persona se ven solo sus carros`() = runTest {
        withOwners()
        viewModel.start("mustang", null)
        advanceUntilIdle()

        viewModel.onPickerOwnerSelected("sofia")

        val picker = viewModel.state.value.picker
        assertThat(picker.selectedOwner?.name).isEqualTo("Sofía")
        assertThat(picker.ownerVehicles.map { it.id }).containsExactly("wrangler", "tesla")
    }

    @Test
    fun `el buscador filtra personas en el paso 1 y carros en el paso 2`() = runTest {
        withOwners()
        viewModel.start("mustang", null)
        advanceUntilIdle()

        viewModel.onPickerQueryChange("sof")
        assertThat(viewModel.state.value.picker.owners.map { it.name }).containsExactly("Sofía")

        viewModel.onPickerOwnerSelected("sofia")
        assertThat(viewModel.state.value.picker.query).isEmpty()
        viewModel.onPickerQueryChange("jeep")
        assertThat(viewModel.state.value.picker.ownerVehicles.map { it.id }).containsExactly("wrangler")
    }

    @Test
    fun `volver regresa a la lista de personas`() = runTest {
        withOwners()
        viewModel.start("mustang", null)
        advanceUntilIdle()
        viewModel.onPickerOwnerSelected("ivan")

        viewModel.onPickerBack()

        assertThat(viewModel.state.value.picker.selectedOwnerKey).isNull()
        assertThat(viewModel.state.value.picker.isOpen).isTrue()
    }

    @Test
    fun `elegir el carro de la persona arma la comparacion`() = runTest {
        withOwners()
        viewModel.start("mustang", null)
        advanceUntilIdle()
        viewModel.onPickerOwnerSelected("ivan")

        viewModel.selectVehicle("ranger")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.picker.isOpen).isFalse()
        assertThat(state.picker.selectedOwnerKey).isNull()
        assertThat(state.comparison?.right?.modelo).isEqualTo("Ranger")
    }

    @Test
    fun `los carros sin duenio salen como catalogo`() = runTest {
        viewModel.start("mustang", null)
        advanceUntilIdle()

        val owners = viewModel.state.value.picker.owners
        assertThat(owners.map { it.key }).containsExactly(PickerOwner.CATALOG_KEY)
        viewModel.onPickerOwnerSelected(PickerOwner.CATALOG_KEY)
        assertThat(viewModel.state.value.picker.ownerVehicles.map { it.id }).containsExactly("wrangler", "tesla")
    }

    private fun vehicle(id: String, brand: String, model: String, hp: String, accel: String, top: String) = Vehicle(
        id = id, brand = brand, model = model, year = 2024, category = "Cat", imageUrl = null,
        potencia = hp, aceleracion = accel, velocidadMaxima = top, traccion = "RWD"
    )

    private fun review(id: String, vehicleId: String, rating: Int, likes: Int, comment: String) = Review(
        id = id, rating = rating, comment = comment, userId = "u$id", vehicleId = vehicleId,
        createdAt = null, likesCount = likes, author = ReviewAuthor("u$id", "Autor"), vehicle = null
    )
}
