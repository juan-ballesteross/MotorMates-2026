package com.example.motormates.ui.aiTuning

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.Build
import com.example.motormates.data.model.BuildDraft
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.PartsCatalog
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningPackage
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.BuildRepository
import com.example.motormates.data.repository.GeneratedImage
import com.example.motormates.data.repository.PartsCatalogRepository
import com.example.motormates.data.repository.TuningException
import com.example.motormates.data.repository.TuningRequest
import com.example.motormates.data.repository.VehicleRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiTuningViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var catalogRepository: PartsCatalogRepository
    private lateinit var aiTuningRepository: AiTuningRepository
    private lateinit var buildRepository: BuildRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AiTuningViewModel

    private val porsche = Vehicle(
        id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = "https://example.com/911.jpg", potencia = "500 hp", aceleracion = "3.4 s",
        velocidadMaxima = "318 km/h", traccion = "RWD", ownerId = "u1"
    )

    private fun part(id: String, category: PartCategory, hp: Double = 0.0, min: Long = 1_000_000, max: Long = 2_000_000, compat: List<String> = emptyList()) =
        Part(id = id, name = id, brand = "", category = category, priceMinCop = min, priceMaxCop = max, hpDeltaPct = hp, compatibleCategories = compat, visualPrompt = id)

    private val rin18 = part("rin18", PartCategory.RINES)
    private val rin19 = part("rin19", PartCategory.RINES)
    private val rinOffroad = part("rinOff", PartCategory.RINES, compat = listOf("SUV"))
    private val escape = part("escape", PartCategory.ESCAPE, hp = 4.0, min = 5_000_000, max = 10_000_000)
    private val ecu = part("ecu", PartCategory.ECU, hp = 15.0)
    private val street = TuningPackage("street", "Street", "", listOf("rinOff", "rin19", "rin18", "escape"))

    private val catalog = PartsCatalog(listOf(rin18, rin19, rinOffroad, escape, ecu), listOf(street))
    private val result = TuningImage(byteArrayOf(9))

    @Before
    fun setUp() {
        vehicleRepository = mockk()
        catalogRepository = mockk()
        aiTuningRepository = mockk()
        buildRepository = mockk()
        authRepository = mockk()

        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche)
        coEvery { catalogRepository.getCatalog() } returns Result.success(catalog)
        coEvery { aiTuningRepository.remainingGenerations() } returns 5
        every { authRepository.getCurrentUser() } returns AuthUser("u1", null, "Ana")

        viewModel = AiTuningViewModel(vehicleRepository, catalogRepository, aiTuningRepository, buildRepository, authRepository)
    }

    private suspend fun loaded() {
        viewModel.load("1")
    }

    // ---------- carga ----------

    @Test
    fun `load filtra el catalogo por la categoria del vehiculo`() = runTest {
        loaded(); advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.loadError).isNull()
        assertThat(state.parts.map { it.id }).doesNotContain("rinOff")
        // El paquete queda solo con las piezas compatibles.
        assertThat(state.packages.single().partIds).containsExactly("rin19", "rin18", "escape").inOrder()
        assertThat(state.isOwner).isTrue()
        assertThat(state.remainingGenerations).isEqualTo(5)
        assertThat(state.selectedCategory).isEqualTo(PartCategory.RINES)
        assertThat(state.summary.before.hp).isEqualTo(500.0)
    }

    @Test
    fun `vehiculo sin foto muestra error de carga`() = runTest {
        coEvery { vehicleRepository.getVehicleById("1") } returns Result.success(porsche.copy(imageUrl = null))

        loaded(); advanceUntilIdle()

        assertThat(viewModel.state.value.loadError).contains("no tiene foto")
        assertThat(viewModel.state.value.canGenerate).isFalse()
    }

    @Test
    fun `si falla el catalogo se muestra el error`() = runTest {
        coEvery { catalogRepository.getCatalog() } returns Result.failure(Exception("No se pudo cargar el catálogo de piezas"))

        loaded(); advanceUntilIdle()

        assertThat(viewModel.state.value.loadError).isEqualTo("No se pudo cargar el catálogo de piezas")
    }

    // ---------- selección ----------

    @Test
    fun `una pieza por categoria - otra la reemplaza y la misma la quita`() = runTest {
        loaded(); advanceUntilIdle()

        viewModel.togglePart("rin18")
        assertThat(viewModel.state.value.selection).containsExactly(PartCategory.RINES, "rin18")

        viewModel.togglePart("rin19")
        assertThat(viewModel.state.value.selection).containsExactly(PartCategory.RINES, "rin19")

        viewModel.togglePart("rin19")
        assertThat(viewModel.state.value.selection).isEmpty()
    }

    @Test
    fun `el resumen se recalcula en vivo con cada pieza`() = runTest {
        loaded(); advanceUntilIdle()

        viewModel.togglePart("escape")
        viewModel.togglePart("ecu")

        val summary = viewModel.state.value.summary
        assertThat(summary.partsCount).isEqualTo(2)
        assertThat(summary.totalMinCop).isEqualTo(6_000_000)
        assertThat(summary.totalMaxCop).isEqualTo(12_000_000)
        assertThat(summary.hpDeltaPct).isEqualTo(19.0)
        assertThat(summary.after.hp).isWithin(1e-9).of(595.0)
    }

    @Test
    fun `el paquete elige la primera pieza compatible por categoria y se puede quitar`() = runTest {
        loaded(); advanceUntilIdle()

        viewModel.applyPackage("street")
        val state = viewModel.state.value
        assertThat(state.selection).containsExactly(PartCategory.RINES, "rin19", PartCategory.ESCAPE, "escape")
        assertThat(state.activePackageId).isEqualTo("street")

        viewModel.applyPackage("street")
        assertThat(viewModel.state.value.selection).isEmpty()
        assertThat(viewModel.state.value.activePackageId).isNull()
    }

    @Test
    fun `tocar una pieza despues de un paquete desactiva el paquete`() = runTest {
        loaded(); advanceUntilIdle()
        viewModel.applyPackage("street")

        viewModel.togglePart("ecu")

        assertThat(viewModel.state.value.activePackageId).isNull()
        assertThat(viewModel.state.value.selection).hasSize(3)
    }

    @Test
    fun `canGenerate depende del modo`() = runTest {
        loaded(); advanceUntilIdle()
        assertThat(viewModel.state.value.canGenerate).isFalse()

        viewModel.togglePart("rin18")
        assertThat(viewModel.state.value.canGenerate).isTrue()

        viewModel.selectMode(TuningMode.FREE)
        assertThat(viewModel.state.value.canGenerate).isFalse()
        viewModel.updateFreeIdea("franjas rojas")
        assertThat(viewModel.state.value.canGenerate).isTrue()

        viewModel.selectMode(TuningMode.OWN_PART)
        assertThat(viewModel.state.value.canGenerate).isFalse()
    }

    @Test
    fun `sin generaciones restantes no se puede generar`() = runTest {
        coEvery { aiTuningRepository.remainingGenerations() } returns 0
        loaded(); advanceUntilIdle()
        viewModel.togglePart("rin18")

        assertThat(viewModel.state.value.canGenerate).isFalse()
        assertThat(viewModel.state.value.hasNoGenerationsLeft).isTrue()
    }

    // ---------- generación ----------

    @Test
    fun `generate exitoso pasa al resultado y pide la ficha tecnica`() = runTest {
        val request = slot<TuningRequest>()
        coEvery { aiTuningRepository.generate(capture(request)) } returns
            Result.success(GeneratedImage(result, "prompt", remainingToday = 4))
        coEvery { aiTuningRepository.explain(any(), any(), any()) } returns
            Result.success(TuningInsights(summary = "Más potencia"))
        loaded(); advanceUntilIdle()
        viewModel.togglePart("ecu")

        viewModel.generate()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.phase).isEqualTo(TuningPhase.RESULT)
        assertThat(state.resultImage).isEqualTo(result)
        assertThat(state.resultParts.map { it.id }).containsExactly("ecu")
        assertThat(state.resultSummary.hpDeltaPct).isEqualTo(15.0)
        assertThat(state.remainingGenerations).isEqualTo(4)
        assertThat(state.insights?.summary).isEqualTo("Más potencia")
        assertThat(request.captured.mode).isEqualTo(TuningMode.CATALOG)
        assertThat(request.captured.parts.map { it.id }).containsExactly("ecu")
    }

    @Test
    fun `en modo libre no se pide ficha tecnica`() = runTest {
        coEvery { aiTuningRepository.generate(any()) } returns Result.success(GeneratedImage(result, "p", 4))
        loaded(); advanceUntilIdle()
        viewModel.selectMode(TuningMode.FREE)
        viewModel.updateFreeIdea("franjas")

        viewModel.generate()
        advanceUntilIdle()

        assertThat(viewModel.state.value.phase).isEqualTo(TuningPhase.RESULT)
        coVerify(exactly = 0) { aiTuningRepository.explain(any(), any(), any()) }
    }

    @Test
    fun `generate fallido vuelve al configurador con el mensaje`() = runTest {
        coEvery { aiTuningRepository.generate(any()) } returns
            Result.failure(TuningException("Llegaste al límite de 5 imágenes por hoy. Vuelve mañana."))
        loaded(); advanceUntilIdle()
        viewModel.togglePart("rin18")

        viewModel.generate()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.phase).isEqualTo(TuningPhase.CONFIGURING)
        assertThat(state.message).contains("límite")
        // La selección no se pierde.
        assertThat(state.selection).containsExactly(PartCategory.RINES, "rin18")
    }

    @Test
    fun `si falla la ficha se puede reintentar sin regenerar la imagen`() = runTest {
        coEvery { aiTuningRepository.generate(any()) } returns Result.success(GeneratedImage(result, "p", 4))
        coEvery { aiTuningRepository.explain(any(), any(), any()) } returns Result.failure(Exception("La AI no está disponible"))
        loaded(); advanceUntilIdle()
        viewModel.togglePart("ecu")
        viewModel.generate(); advanceUntilIdle()
        assertThat(viewModel.state.value.insightsError).isEqualTo("La AI no está disponible")

        coEvery { aiTuningRepository.explain(any(), any(), any()) } returns Result.success(TuningInsights(summary = "ok"))
        viewModel.loadInsights(); advanceUntilIdle()

        assertThat(viewModel.state.value.insights?.summary).isEqualTo("ok")
        assertThat(viewModel.state.value.insightsError).isNull()
        coVerify(exactly = 1) { aiTuningRepository.generate(any()) }
    }

    @Test
    fun `refine reemplaza la imagen y limpia el campo`() = runTest {
        val refined = TuningImage(byteArrayOf(10))
        coEvery { aiTuningRepository.generate(any()) } returns Result.success(GeneratedImage(result, "p", 4))
        coEvery { aiTuningRepository.explain(any(), any(), any()) } returns Result.success(TuningInsights())
        coEvery { aiTuningRepository.refine("más bajo") } returns Result.success(GeneratedImage(refined, "p2", 3))
        loaded(); advanceUntilIdle()
        viewModel.togglePart("rin18")
        viewModel.generate(); advanceUntilIdle()

        viewModel.updateRefineText("más bajo")
        viewModel.refine(); advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.resultImage).isEqualTo(refined)
        assertThat(state.refineText).isEmpty()
        assertThat(state.remainingGenerations).isEqualTo(3)
        assertThat(state.isRefining).isFalse()
    }

    // ---------- guardar ----------

    @Test
    fun `save guarda el build con las piezas y la ficha del resultado`() = runTest {
        val insights = TuningInsights(summary = "ok")
        coEvery { aiTuningRepository.generate(any()) } returns Result.success(GeneratedImage(result, "p", 4))
        coEvery { aiTuningRepository.explain(any(), any(), any()) } returns Result.success(insights)
        val draft = slot<BuildDraft>()
        val saved = Build(
            id = "b1", vehicleId = "1", vehicleName = "Porsche 911 GT3", ownerId = "u1", ownerName = "Ana",
            baseImageUrl = porsche.imageUrl, resultImageUrl = "https://storage/b1.jpg", partIds = listOf("escape"),
            partNames = listOf("escape"), mode = TuningMode.CATALOG, prompt = null, summary = TuningSummary(),
            insights = insights, published = true, createdAt = 1
        )
        coEvery { buildRepository.save(capture(draft), result, true) } returns Result.success(saved)
        loaded(); advanceUntilIdle()
        viewModel.togglePart("escape")
        viewModel.generate(); advanceUntilIdle()

        viewModel.save(publish = true); advanceUntilIdle()

        assertThat(draft.captured.parts.map { it.id }).containsExactly("escape")
        assertThat(draft.captured.vehicleName).isEqualTo("Porsche 911 GT3")
        assertThat(draft.captured.insights).isEqualTo(insights)
        assertThat(draft.captured.summary.totalMaxCop).isEqualTo(10_000_000)
        assertThat(viewModel.state.value.savedBuild?.id).isEqualTo("b1")
        assertThat(viewModel.state.value.message).isEqualTo("Build publicado en la comunidad")
    }

    @Test
    fun `cambiar de seleccion despues de generar no altera el resultado guardable`() = runTest {
        coEvery { aiTuningRepository.generate(any()) } returns Result.success(GeneratedImage(result, "p", 4))
        coEvery { aiTuningRepository.explain(any(), any(), any()) } returns Result.success(TuningInsights())
        loaded(); advanceUntilIdle()
        viewModel.togglePart("escape")
        viewModel.generate(); advanceUntilIdle()

        viewModel.backToConfigurator()
        viewModel.togglePart("ecu")

        val state = viewModel.state.value
        assertThat(state.phase).isEqualTo(TuningPhase.CONFIGURING)
        assertThat(state.resultParts.map { it.id }).containsExactly("escape")
        assertThat(state.selection).hasSize(2)
    }
}
