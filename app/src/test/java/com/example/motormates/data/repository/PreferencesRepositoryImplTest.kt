package com.example.motormates.data.repository

import com.example.motormates.data.dto.CarRefDto
import com.example.motormates.data.dto.PreferencesDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.CarRef
import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.PreferencesDataSource
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class PreferencesRepositoryImplTest {

    private val dataSource: PreferencesDataSource = mockk()
    private val auth: AuthDataSource = mockk()
    private val time: TimeSource = mockk()
    private lateinit var repository: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        every { auth.getCurrentUser() } returns AuthUser(id = "uid", email = null, displayName = null)
        every { time.nowMillis() } returns 1_000L
        repository = PreferencesRepositoryImpl(dataSource, auth, time)
    }

    @Test
    fun `sin documento devuelve preferencias vacias y sin decidir`() = runTest {
        coEvery { dataSource.get("uid") } returns null

        val preferences = repository.getMine().getOrThrow()

        assertThat(preferences).isEqualTo(UserPreferences())
        assertThat(preferences.isDecided).isFalse()
    }

    @Test
    fun `convierte el documento y descarta intereses desconocidos y carros incompletos`() = runTest {
        coEvery { dataSource.get("uid") } returns PreferencesDto(
            categories = listOf(" SUV ", "SUV", ""),
            interests = listOf("MODIFY", "NO_EXISTE"),
            city = "Cali",
            car = CarRefDto(brand = "Mazda", model = "", year = 2020),
            completed = true
        )

        val preferences = repository.getMine().getOrThrow()

        assertThat(preferences.categories).containsExactly("SUV")
        assertThat(preferences.interests).containsExactly(Interest.MODIFY)
        assertThat(preferences.car).isNull()
        assertThat(preferences.isDecided).isTrue()
    }

    @Test
    fun `guardar marca completado con la hora y escribe la forma de Firestore`() = runTest {
        val saved = slot<PreferencesDto>()
        coEvery { dataSource.save("uid", capture(saved)) } returns Unit

        val result = repository.save(
            UserPreferences(
                categories = listOf("Deportivo"),
                interests = listOf(Interest.PHOTO),
                car = CarRef("Mazda", "3", 2020),
                skipped = true
            )
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(saved.captured.completed).isTrue()
        assertThat(saved.captured.skipped).isFalse()
        assertThat(saved.captured.updatedAt).isEqualTo(1_000L)
        assertThat(saved.captured.interests).containsExactly("PHOTO")
        assertThat(saved.captured.toMap()["car"]).isEqualTo(mapOf("brand" to "Mazda", "model" to "3", "year" to 2020))
    }

    @Test
    fun `saltar conserva lo que habia y marca skipped`() = runTest {
        val saved = slot<PreferencesDto>()
        coEvery { dataSource.get("uid") } returns PreferencesDto(categories = listOf("SUV"))
        coEvery { dataSource.save("uid", capture(saved)) } returns Unit

        repository.skip()

        assertThat(saved.captured.categories).containsExactly("SUV")
        assertThat(saved.captured.skipped).isTrue()
        assertThat(saved.captured.completed).isFalse()
    }

    @Test
    fun `sin sesion no toca Firestore`() = runTest {
        every { auth.getCurrentUser() } returns null

        val result = repository.save(UserPreferences())

        assertThat(result.exceptionOrNull()?.message).isEqualTo(PreferencesRepositoryImpl.NO_SESSION)
        coVerify(exactly = 0) { dataSource.save(any(), any()) }
    }

    @Test
    fun `un error al guardar se traduce a espanol`() = runTest {
        // Con un error genérico: FirebaseFirestoreException.Code no se puede
        // crear en la JVM (usa SparseArray de Android), igual que en
        // EventRepositoryImplTest.
        coEvery { dataSource.save(any(), any()) } throws IllegalStateException("boom")

        val result = repository.save(UserPreferences())

        assertThat(result.exceptionOrNull()?.message).isEqualTo(PreferencesRepositoryImpl.SAVE_FAILED)
    }

    @Test
    fun `observar emite vacio si el listener falla`() = runTest {
        every { dataSource.observe("uid") } returns flow {
            emit(PreferencesDto(categories = listOf("SUV")))
            throw IllegalStateException("permiso")
        }

        val emitted = repository.observeMine().toList()

        assertThat(emitted).containsExactly(
            UserPreferences(categories = listOf("SUV")),
            UserPreferences()
        ).inOrder()
    }

    @Test
    fun `observar sin sesion emite vacio`() = runTest {
        every { auth.getCurrentUser() } returns null
        every { dataSource.observe(any()) } returns flowOf()

        assertThat(repository.observeMine().toList()).containsExactly(UserPreferences())
    }
}
