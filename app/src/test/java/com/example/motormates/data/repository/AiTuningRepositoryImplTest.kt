package com.example.motormates.data.repository

import com.example.motormates.data.model.AuthUser
import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.source.AiModelConfig
import com.example.motormates.data.source.AiUsageDataSource
import com.example.motormates.data.source.GeminiTuningDataSource
import com.example.motormates.data.source.ImageEditSession
import com.example.motormates.data.source.NoImageReturnedException
import com.example.motormates.data.source.TuningImageSource
import com.example.motormates.data.util.TimeSource
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AiTuningRepositoryImplTest {

    private lateinit var gemini: GeminiTuningDataSource
    private lateinit var images: TuningImageSource
    private lateinit var usage: AiUsageDataSource
    private lateinit var config: AiModelConfig
    private lateinit var authRepository: AuthRepository
    private lateinit var timeSource: TimeSource
    private lateinit var session: ImageEditSession
    private lateinit var repository: AiTuningRepositoryImpl

    private val today = LocalDate.of(2026, 11, 17)
    private val baseImage = TuningImage(byteArrayOf(1))
    private val wheelImage = TuningImage(byteArrayOf(2))
    private val resultImage = TuningImage(byteArrayOf(9))

    private val porsche = Vehicle(
        id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = "https://example.com/911.jpg", potencia = "502 hp", aceleracion = "3.4 s",
        velocidadMaxima = "318 km/h", traccion = "RWD"
    )
    private val wheel = Part(
        id = "rin", name = "Rin 18", brand = "OZ", category = PartCategory.RINES,
        imageUrl = "https://example.com/rin.jpg", priceMinCop = 1, priceMaxCop = 2,
        visualPrompt = "black wheels"
    )
    private val wing = Part(
        id = "ala", name = "Ala GT", brand = "", category = PartCategory.AERO,
        priceMinCop = 1, priceMaxCop = 2, visualPrompt = "a GT wing"
    )

    @Before
    fun setUp() {
        gemini = mockk()
        images = mockk()
        usage = mockk()
        config = mockk()
        authRepository = mockk()
        timeSource = mockk()
        session = mockk()

        every { authRepository.getCurrentUser() } returns AuthUser("u1", "u1@test.com", "Ana")
        every { timeSource.nowMillis() } returns
            today.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        coEvery { config.dailyGenerationLimit() } returns 5
        coEvery { usage.tryConsume("u1", "2026-11-17", 5) } returns true
        coEvery { usage.usedToday("u1", "2026-11-17") } returns 1
        coEvery { images.loadFromUrl(porsche.imageUrl!!) } returns baseImage
        coEvery { images.loadFromUrl(wheel.imageUrl!!) } returns wheelImage
        coEvery { gemini.startImageSession() } returns session
        coEvery { session.send(any(), any()) } returns resultImage

        repository = AiTuningRepositoryImpl(gemini, images, usage, config, authRepository, timeSource)
    }

    @Test
    fun `generate en catalogo manda la foto del carro y las fotos de referencia`() = runTest {
        val prompt = slot<String>()
        val sent = slot<List<TuningImage>>()
        coEvery { session.send(capture(prompt), capture(sent)) } returns resultImage

        val result = repository.generate(TuningRequest(porsche, TuningMode.CATALOG, parts = listOf(wheel, wing)))

        assertThat(result.isSuccess).isTrue()
        val generated = result.getOrThrow()
        assertThat(generated.image).isEqualTo(resultImage)
        assertThat(generated.remainingToday).isEqualTo(4)
        // Imagen 1 = carro, imagen 2 = rin (la única pieza con foto).
        assertThat(sent.captured).containsExactly(baseImage, wheelImage).inOrder()
        assertThat(prompt.captured).contains("black wheels. Use image 2 as the exact reference")
        assertThat(prompt.captured).contains("a GT wing.")
        coVerify(exactly = 1) { usage.tryConsume("u1", "2026-11-17", 5) }
    }

    @Test
    fun `si una foto de referencia falla la pieza se describe solo con texto`() = runTest {
        coEvery { images.loadFromUrl(wheel.imageUrl!!) } throws IOException("404")
        val sent = slot<List<TuningImage>>()
        coEvery { session.send(any(), capture(sent)) } returns resultImage

        val result = repository.generate(TuningRequest(porsche, TuningMode.CATALOG, parts = listOf(wheel)))

        assertThat(result.isSuccess).isTrue()
        assertThat(sent.captured).containsExactly(baseImage)
    }

    @Test
    fun `una foto de un carro entero se ve en la tarjeta pero no se manda como referencia`() = runTest {
        val lifted = wheel.copy(id = "lift", imageUrl = "https://example.com/lifted-truck.jpg", imageIsReference = false)
        val sent = slot<List<TuningImage>>()
        coEvery { session.send(any(), capture(sent)) } returns resultImage

        val result = repository.generate(TuningRequest(porsche, TuningMode.CATALOG, parts = listOf(lifted)))

        assertThat(result.isSuccess).isTrue()
        assertThat(sent.captured).containsExactly(baseImage)
        coVerify(exactly = 0) { images.loadFromUrl(lifted.imageUrl!!) }
    }

    @Test
    fun `al llegar al limite diario no se llama al modelo`() = runTest {
        coEvery { usage.tryConsume(any(), any(), any()) } returns false

        val result = repository.generate(TuningRequest(porsche, TuningMode.CATALOG, parts = listOf(wing)))

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("límite de 5 imágenes")
        coVerify(exactly = 0) { gemini.startImageSession() }
    }

    @Test
    fun `si la foto del carro no descarga no se gasta una generacion`() = runTest {
        coEvery { images.loadFromUrl(porsche.imageUrl!!) } throws IOException("sin red")

        val result = repository.generate(TuningRequest(porsche, TuningMode.CATALOG, parts = listOf(wing)))

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("No se pudo descargar la foto")
        coVerify(exactly = 0) { usage.tryConsume(any(), any(), any()) }
    }

    @Test
    fun `catalogo sin piezas o idea vacia fallan con mensaje claro`() = runTest {
        val sinPiezas = repository.generate(TuningRequest(porsche, TuningMode.CATALOG))
        val sinIdea = repository.generate(TuningRequest(porsche, TuningMode.FREE, freeIdea = "  "))
        val sinFoto = repository.generate(TuningRequest(porsche, TuningMode.OWN_PART))

        assertThat(sinPiezas.exceptionOrNull()).hasMessageThat().isEqualTo("Elige al menos una pieza")
        assertThat(sinIdea.exceptionOrNull()).hasMessageThat().isEqualTo("Describe tu idea primero")
        assertThat(sinFoto.exceptionOrNull()).hasMessageThat().contains("foto de la pieza")
        coVerify(exactly = 0) { usage.tryConsume(any(), any(), any()) }
    }

    @Test
    fun `vehiculo sin foto no se puede modificar`() = runTest {
        val result = repository.generate(
            TuningRequest(porsche.copy(imageUrl = null), TuningMode.FREE, freeIdea = "franjas")
        )

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("no tiene foto")
    }

    @Test
    fun `modo pieza propia manda la foto de la pieza como imagen 2`() = runTest {
        val partPhoto = TuningImage(byteArrayOf(7))
        val sent = slot<List<TuningImage>>()
        coEvery { session.send(any(), capture(sent)) } returns resultImage

        repository.generate(
            TuningRequest(porsche, TuningMode.OWN_PART, ownPartImage = partPhoto, ownPartDescription = "rin")
        )

        assertThat(sent.captured).containsExactly(baseImage, partPhoto).inOrder()
    }

    @Test
    fun `si el modelo responde sin imagen se explica en espanol`() = runTest {
        coEvery { session.send(any(), any()) } throws NoImageReturnedException("I can't")

        val result = repository.generate(TuningRequest(porsche, TuningMode.FREE, freeIdea = "franjas"))

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("La AI no pudo generar la imagen")
    }

    @Test
    fun `refine sin una imagen previa falla`() = runTest {
        val result = repository.refine("más bajo")

        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Primero genera una imagen")
    }

    @Test
    fun `refine continua la misma sesion sin reenviar imagenes`() = runTest {
        repository.generate(TuningRequest(porsche, TuningMode.FREE, freeIdea = "franjas"))
        val refined = TuningImage(byteArrayOf(10))
        coEvery { session.send(match { it.contains("más bajo") }, emptyList()) } returns refined

        val result = repository.refine("más bajo")

        assertThat(result.getOrThrow().image).isEqualTo(refined)
        coVerify(exactly = 1) { gemini.startImageSession() }
        coVerify(exactly = 2) { usage.tryConsume(any(), any(), any()) }
    }

    @Test
    fun `explain convierte el JSON del modelo en la ficha`() = runTest {
        coEvery { gemini.generateInsightsJson(any()) } returns """
            {"summary":"Más rápido","handling":"Más firme","consumption":"Igual","warranty":"Revisar",
             "legal":["Reportar cambio de color"],"tips":["Alinear"]}
        """.trimIndent()

        val insights = repository.explain(porsche, listOf(wing), TuningSummary()).getOrThrow()

        assertThat(insights.summary).isEqualTo("Más rápido")
        assertThat(insights.legal).containsExactly("Reportar cambio de color")
        assertThat(insights.tips).containsExactly("Alinear")
    }

    @Test
    fun `explain con JSON invalido falla con mensaje claro`() = runTest {
        coEvery { gemini.generateInsightsJson(any()) } returns "esto no es json"

        val result = repository.explain(porsche, listOf(wing), TuningSummary())

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("formato inesperado")
    }

    @Test
    fun `livePrice devuelve la respuesta con fuentes`() = runTest {
        val answer = GroundedAnswer("Entre 4 y 7 millones", searchEntryPointHtml = "<div/>")
        coEvery { gemini.searchGrounded(match { it.contains("Rin 18") }, null) } returns answer

        val result = repository.livePrice(wheel)

        assertThat(result.getOrThrow()).isEqualTo(answer)
    }

    @Test
    fun `los errores de red se traducen`() = runTest {
        coEvery { gemini.searchGrounded(any(), any()) } throws IOException("timeout")

        val result = repository.livePrice(wheel)

        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Sin conexión a internet, verifica tu red")
    }

    @Test
    fun `sin sesion no se puede generar`() = runTest {
        every { authRepository.getCurrentUser() } returns null

        val result = repository.generate(TuningRequest(porsche, TuningMode.FREE, freeIdea = "franjas"))

        assertThat(result.exceptionOrNull()).hasMessageThat().contains("iniciar sesión")
        assertThat(repository.remainingGenerations()).isNull()
    }

    @Test
    fun `remainingGenerations resta lo usado al limite`() = runTest {
        coEvery { usage.usedToday("u1", "2026-11-17") } returns 3

        assertThat(repository.remainingGenerations()).isEqualTo(2)
    }

    @Test
    fun `sin limite configurado no se cuenta nada y no hay restantes`() = runTest {
        coEvery { config.dailyGenerationLimit() } returns null
        coEvery { session.send(any(), any()) } returns resultImage

        val generated = repository.generate(TuningRequest(porsche, TuningMode.CATALOG, parts = listOf(wheel))).getOrThrow()

        assertThat(generated.remainingToday).isNull()
        assertThat(repository.remainingGenerations()).isNull()
        coVerify(exactly = 0) { usage.tryConsume(any(), any(), any()) }
    }
}
