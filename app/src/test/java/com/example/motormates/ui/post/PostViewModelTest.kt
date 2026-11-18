package com.example.motormates.ui.post

import android.net.Uri
import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.model.VehicleFormField
import com.example.motormates.data.repository.GarageRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Formulario para publicar un vehículo, con el repositorio mockeado. */
@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var garageRepository: GarageRepository
    private lateinit var viewModel: PostViewModel

    private val published = Vehicle(
        id = "nuevo", brand = "Mazda", model = "MX-5", year = 2020, category = "Deportivo",
        imageUrl = null, potencia = "181 hp", aceleracion = null, velocidadMaxima = null, traccion = null,
        ownerId = "me"
    )

    @Before
    fun setUp() {
        garageRepository = mockk()
        viewModel = PostViewModel(garageRepository)
    }

    private fun fillMinimum() {
        viewModel.onBrandChange("Mazda")
        viewModel.onModelChange("MX-5")
        viewModel.onYearChange("2020")
        viewModel.onCategorySelect("Deportivo")
    }

    @Test
    fun `publicar se habilita solo con marca modelo ano y categoria`() {
        assertThat(viewModel.uiState.value.canPublish).isFalse()

        fillMinimum()

        assertThat(viewModel.uiState.value.canPublish).isTrue()
    }

    @Test
    fun `el ano solo acepta hasta cuatro digitos`() {
        viewModel.onYearChange("20a2015")

        assertThat(viewModel.uiState.value.form.year).isEqualTo("2020")
    }

    @Test
    fun `tocar la categoria elegida la quita`() {
        viewModel.onCategorySelect("SUV")
        viewModel.onCategorySelect("SUV")

        assertThat(viewModel.uiState.value.form.category).isNull()
    }

    @Test
    fun `publicar con datos invalidos muestra errores y no llama al repositorio`() = runTest {
        fillMinimum()
        viewModel.onYearChange("1500")

        viewModel.publish()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errors).containsKey(VehicleFormField.YEAR)
        coVerify(exactly = 0) { garageRepository.publishVehicle(any(), any()) }
    }

    @Test
    fun `corregir un campo borra su error`() = runTest {
        fillMinimum()
        viewModel.onYearChange("1500")
        viewModel.publish()

        viewModel.onYearChange("2020")

        assertThat(viewModel.uiState.value.errors).doesNotContainKey(VehicleFormField.YEAR)
    }

    @Test
    fun `publicar bien manda el borrador y la foto y deja el id para navegar`() = runTest {
        val draft = slot<VehicleDraft>()
        val photo = mockk<Uri>()
        coEvery { garageRepository.publishVehicle(capture(draft), photo) } returns Result.success(published)
        fillMinimum()
        viewModel.onPotenciaChange("181")
        viewModel.onImagePicked(photo)

        viewModel.publish()
        advanceUntilIdle()

        assertThat(draft.captured.potencia).isEqualTo("181 hp")
        val state = viewModel.uiState.value
        assertThat(state.publishedVehicleId).isEqualTo("nuevo")
        // El formulario queda limpio para la próxima vez.
        assertThat(state.form.brand).isEmpty()
        assertThat(state.imageUri).isNull()
    }

    @Test
    fun `si falla la publicacion se conserva el formulario y se muestra el error`() = runTest {
        coEvery { garageRepository.publishVehicle(any(), any()) } returns
            Result.failure(Exception("No se pudo subir la foto del vehículo"))
        fillMinimum()

        viewModel.publish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isPublishing).isFalse()
        assertThat(state.errorMessage).isEqualTo("No se pudo subir la foto del vehículo")
        assertThat(state.form.brand).isEqualTo("Mazda")
        assertThat(state.publishedVehicleId).isNull()
    }

    @Test
    fun `cerrar la galeria sin elegir conserva la foto anterior`() {
        val photo = mockk<Uri>()
        viewModel.onImagePicked(photo)

        viewModel.onImagePicked(null)

        assertThat(viewModel.uiState.value.imageUri).isEqualTo(photo)
    }

    // ---------- Editar ----------

    private val mine = published.copy(
        id = "mio", imageUrl = "https://vieja.jpg", aceleracion = "6.5 s", traccion = "RWD"
    )

    @Test
    fun `editar prellena el formulario con el vehiculo`() = runTest {
        coEvery { garageRepository.getMyVehicle("mio") } returns Result.success(mine)

        viewModel.startEditing("mio")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.form.brand).isEqualTo("Mazda")
        assertThat(state.form.potenciaHp).isEqualTo("181")
        assertThat(state.form.aceleracionS).isEqualTo("6.5")
        assertThat(state.photoModel).isEqualTo("https://vieja.jpg")
        assertThat(state.canPublish).isTrue()
    }

    @Test
    fun `si el vehiculo no es mio muestra el error y no deja guardar`() = runTest {
        coEvery { garageRepository.getMyVehicle("ajeno") } returns
            Result.failure(Exception("Solo el dueño puede editar este vehículo"))

        viewModel.startEditing("ajeno")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loadError).isEqualTo("Solo el dueño puede editar este vehículo")
        assertThat(viewModel.uiState.value.canPublish).isFalse()
    }

    @Test
    fun `guardar sin cambiar la foto conserva la que tenia`() = runTest {
        coEvery { garageRepository.getMyVehicle("mio") } returns Result.success(mine)
        coEvery { garageRepository.updateVehicle("mio", any(), null, "https://vieja.jpg") } returns Result.success(Unit)
        viewModel.startEditing("mio")
        advanceUntilIdle()

        viewModel.onModelChange("MX-5 RF")
        viewModel.publish()
        advanceUntilIdle()

        coVerify(exactly = 1) { garageRepository.updateVehicle("mio", any(), null, "https://vieja.jpg") }
        coVerify(exactly = 0) { garageRepository.publishVehicle(any(), any()) }
        assertThat(viewModel.uiState.value.publishedVehicleId).isEqualTo("mio")
    }

    @Test
    fun `quitar la foto al editar guarda el vehiculo sin foto`() = runTest {
        coEvery { garageRepository.getMyVehicle("mio") } returns Result.success(mine)
        coEvery { garageRepository.updateVehicle("mio", any(), null, null) } returns Result.success(Unit)
        viewModel.startEditing("mio")
        advanceUntilIdle()

        viewModel.removeImage()
        viewModel.publish()
        advanceUntilIdle()

        coVerify(exactly = 1) { garageRepository.updateVehicle("mio", any(), null, null) }
    }

    @Test
    fun `eliminar pide confirmacion y luego avisa para cerrar la pantalla`() = runTest {
        coEvery { garageRepository.getMyVehicle("mio") } returns Result.success(mine)
        coEvery { garageRepository.deleteVehicle("mio") } returns Result.success(Unit)
        viewModel.startEditing("mio")
        advanceUntilIdle()

        viewModel.askDelete()
        assertThat(viewModel.uiState.value.showDeleteConfirm).isTrue()
        coVerify(exactly = 0) { garageRepository.deleteVehicle(any()) }

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.wasDeleted).isTrue()
        assertThat(viewModel.uiState.value.showDeleteConfirm).isFalse()
    }

    @Test
    fun `al publicar uno nuevo no se puede eliminar`() {
        viewModel.askDelete()

        assertThat(viewModel.uiState.value.showDeleteConfirm).isFalse()
    }
}
