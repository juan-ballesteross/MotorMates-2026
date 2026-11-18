package com.example.motormates.data.repository

import android.net.Uri
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.dto.VehicleFirestoreDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.NewVehicle
import com.example.motormates.data.model.VehicleDraft
import com.example.motormates.data.remote.datasource.VehicleRemoteDataSource
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.UserDataSource
import com.example.motormates.data.source.VehicleImageDataSource
import com.example.motormates.data.source.VehiclePublishDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Publicar un vehículo: el dueño es la sesión y la foto se sube antes de guardar. */
class GarageRepositoryImplTest {

    private val publishDataSource = mockk<VehiclePublishDataSource>()
    private val imageDataSource = mockk<VehicleImageDataSource>()
    private val userDataSource = mockk<UserDataSource>()
    private val authDataSource = mockk<AuthDataSource>()
    private val vehicleDataSource = mockk<VehicleRemoteDataSource>()

    private val repository = GarageRepositoryImpl(
        publishDataSource, imageDataSource, vehicleDataSource, userDataSource, authDataSource
    )

    private val draft = VehicleDraft(
        brand = "Mazda", model = "MX-5", year = 2020, category = "Deportivo",
        potencia = "181 hp", aceleracion = null, velocidadMaxima = null, traccion = "RWD"
    )

    private val created = slot<NewVehicle>()

    @Before
    fun setUp() {
        every { authDataSource.getCurrentUser() } returns AuthUser(id = "me", email = "d@x.com", displayName = "Diego")
        coEvery { userDataSource.getUser("me") } returns UserDto(id = "me", fullName = "Diego Melgarejo", avatarUrl = "https://a/me.jpg")
        coEvery { publishDataSource.createVehicle(capture(created)) } answers {
            val v = created.captured
            VehicleFirestoreDto(
                id = "nuevo", brand = v.draft.brand, model = v.draft.model, year = v.draft.year,
                category = v.draft.category, imageUrl = v.imageUrl, potencia = v.draft.potencia,
                traccion = v.draft.traccion, ownerId = v.ownerId
            )
        }
    }

    @Test
    fun `sin sesion no publica`() = runTest {
        every { authDataSource.getCurrentUser() } returns null

        val result = repository.publishVehicle(draft, null)

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { publishDataSource.createVehicle(any()) }
    }

    @Test
    fun `el duenio es el usuario de la sesion con su nombre y foto del perfil`() = runTest {
        val result = repository.publishVehicle(draft, null)

        assertThat(result.getOrNull()?.ownerId).isEqualTo("me")
        assertThat(created.captured.ownerId).isEqualTo("me")
        assertThat(created.captured.ownerName).isEqualTo("Diego Melgarejo")
        assertThat(created.captured.ownerAvatarUrl).isEqualTo("https://a/me.jpg")
        assertThat(created.captured.imageUrl).isNull()
    }

    @Test
    fun `con foto la sube primero y guarda su url`() = runTest {
        val photo = mockk<Uri>()
        coEvery { imageDataSource.uploadVehicleImage("me", photo) } returns "https://storage/mx5.jpg"

        val result = repository.publishVehicle(draft, photo)

        assertThat(created.captured.imageUrl).isEqualTo("https://storage/mx5.jpg")
        assertThat(result.getOrNull()?.imageUrl).isEqualTo("https://storage/mx5.jpg")
    }

    @Test
    fun `si falla la subida de la foto no se crea el vehiculo`() = runTest {
        val photo = mockk<Uri>()
        coEvery { imageDataSource.uploadVehicleImage(any(), any()) } throws RuntimeException("sin red")

        val result = repository.publishVehicle(draft, photo)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("No se pudo publicar el vehículo")
        coVerify(exactly = 0) { publishDataSource.createVehicle(any()) }
    }

    @Test
    fun `sin perfil en la base usa el nombre de la cuenta`() = runTest {
        coEvery { userDataSource.getUser("me") } returns null

        repository.publishVehicle(draft, null)

        assertThat(created.captured.ownerName).isEqualTo("Diego")
    }

    // ---------- Editar y eliminar ----------

    @Test
    fun `solo se puede abrir para editar un vehiculo propio`() = runTest {
        coEvery { vehicleDataSource.getVehicleById("mio") } returns VehicleFirestoreDto(id = "mio", brand = "Mazda", ownerId = "me")
        coEvery { vehicleDataSource.getVehicleById("ajeno") } returns VehicleFirestoreDto(id = "ajeno", brand = "Jeep", ownerId = "otro")

        assertThat(repository.getMyVehicle("mio").getOrNull()?.brand).isEqualTo("Mazda")
        assertThat(repository.getMyVehicle("ajeno").exceptionOrNull()?.message)
            .isEqualTo("Solo el dueño puede editar este vehículo")
    }

    @Test
    fun `editar sin foto nueva conserva la que tenia`() = runTest {
        coEvery { publishDataSource.updateVehicle("mio", draft, "https://vieja.jpg") } just runs

        val result = repository.updateVehicle("mio", draft, newImageUri = null, keepImageUrl = "https://vieja.jpg")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { imageDataSource.uploadVehicleImage(any(), any()) }
    }

    @Test
    fun `editar con foto nueva la sube y reemplaza la anterior`() = runTest {
        val photo = mockk<Uri>()
        coEvery { imageDataSource.uploadVehicleImage("me", photo) } returns "https://nueva.jpg"
        coEvery { publishDataSource.updateVehicle("mio", draft, "https://nueva.jpg") } just runs

        repository.updateVehicle("mio", draft, newImageUri = photo, keepImageUrl = "https://vieja.jpg")

        coVerify(exactly = 1) { publishDataSource.updateVehicle("mio", draft, "https://nueva.jpg") }
    }

    @Test
    fun `si falla eliminar devuelve un mensaje claro`() = runTest {
        coEvery { publishDataSource.deleteVehicle("mio") } throws RuntimeException("x")

        val result = repository.deleteVehicle("mio")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("No se pudo eliminar el vehículo")
    }
}
