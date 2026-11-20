package com.example.motormates.data.repository

import com.example.motormates.data.dto.BuildFirestoreDto
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.BuildDraft
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.VehicleSpecs
import com.example.motormates.data.source.BuildDataSource
import com.example.motormates.data.util.SpecCalculator
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BuildRepositoryImplTest {

    private lateinit var dataSource: BuildDataSource
    private lateinit var authRepository: AuthRepository
    private lateinit var timeSource: TimeSource
    private lateinit var repository: BuildRepositoryImpl

    private val image = TuningImage(byteArrayOf(1, 2, 3))
    private val wheel = Part(
        id = "rin", name = "Rin 18", brand = "OZ", category = PartCategory.RINES,
        priceMinCop = 4_000_000, priceMaxCop = 7_000_000, weightDeltaKg = -6.0
    )
    private val summary = SpecCalculator.calculate(
        VehicleSpecs(hp = 300.0, weightKg = 1400.0, zeroTo100 = 6.0), listOf(wheel)
    )
    private val draft = BuildDraft(
        vehicleId = "1",
        vehicleName = "Porsche 911 GT3",
        baseImageUrl = "https://example.com/911.jpg",
        parts = listOf(wheel),
        mode = TuningMode.CATALOG,
        prompt = null,
        summary = summary,
        insights = TuningInsights(summary = "Más liviano", legal = listOf("Nada"))
    )

    @Before
    fun setUp() {
        dataSource = mockk()
        authRepository = mockk()
        timeSource = mockk()
        every { authRepository.getCurrentUser() } returns AuthUser("u1", "u1@test.com", "Ana")
        every { timeSource.nowMillis() } returns 1_000L
        every { dataSource.newBuildId() } returns "b1"
        coEvery { dataSource.uploadImage("u1", "b1", image) } returns "https://storage/b1.jpg"
        coEvery { dataSource.save(any()) } just runs
        repository = BuildRepositoryImpl(dataSource, authRepository, timeSource)
    }

    @Test
    fun `save sube la imagen y guarda el build a nombre del usuario`() = runTest {
        val saved = slot<BuildFirestoreDto>()
        coEvery { dataSource.save(capture(saved)) } just runs

        val build = repository.save(draft, image, publish = true).getOrThrow()

        assertThat(build.id).isEqualTo("b1")
        assertThat(build.resultImageUrl).isEqualTo("https://storage/b1.jpg")
        assertThat(build.published).isTrue()
        with(saved.captured) {
            assertThat(ownerId).isEqualTo("u1")
            assertThat(ownerName).isEqualTo("Ana")
            assertThat(partIds).containsExactly("rin")
            assertThat(partNames).containsExactly("Rin 18")
            assertThat(totalMinCop).isEqualTo(4_000_000)
            assertThat(totalMaxCop).isEqualTo(7_000_000)
            assertThat(weightDeltaKg).isEqualTo(-6.0)
            assertThat(specsAfter.weightKg).isEqualTo(1394.0)
            assertThat(insights?.summary).isEqualTo("Más liviano")
            assertThat(createdAt).isEqualTo(1_000L)
        }
    }

    @Test
    fun `el build guardado conserva la ficha y el resumen al leerlo`() = runTest {
        val saved = slot<BuildFirestoreDto>()
        coEvery { dataSource.save(capture(saved)) } just runs
        repository.save(draft, image, publish = false)
        coEvery { dataSource.getById("b1") } returns saved.captured

        val build = repository.getBuild("b1").getOrThrow()

        assertThat(build.summary.totalMaxCop).isEqualTo(7_000_000)
        assertThat(build.summary.after.weightKg).isEqualTo(1394.0)
        assertThat(build.insights?.legal).containsExactly("Nada")
        assertThat(build.mode).isEqualTo(TuningMode.CATALOG)
        assertThat(build.published).isFalse()
    }

    @Test
    fun `sin sesion no se sube nada`() = runTest {
        every { authRepository.getCurrentUser() } returns null

        val result = repository.save(draft, image, publish = false)

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("iniciar sesión")
        coVerify(exactly = 0) { dataSource.uploadImage(any(), any(), any()) }
    }

    @Test
    fun `si la subida falla no se escribe el documento`() = runTest {
        coEvery { dataSource.uploadImage(any(), any(), any()) } throws RuntimeException("boom")

        val result = repository.save(draft, image, publish = false)

        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("No se pudo guardar el build")
        coVerify(exactly = 0) { dataSource.save(any()) }
    }

    @Test
    fun `getBuild de un id que no existe da mensaje claro`() = runTest {
        coEvery { dataSource.getById("x") } returns null

        assertThat(repository.getBuild("x").exceptionOrNull()).hasMessageThat().contains("no existe")
    }

    @Test
    fun `getMyBuilds trae los del usuario actual`() = runTest {
        coEvery { dataSource.getByOwner("u1") } returns listOf(BuildFirestoreDto(id = "b9", ownerId = "u1"))

        assertThat(repository.getMyBuilds().getOrThrow().map { it.id }).containsExactly("b9")
    }
}
