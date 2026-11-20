package com.example.motormates.ui.aiTuning

import com.example.motormates.MainDispatcherRule
import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.data.repository.GeneratedImage
import com.example.motormates.data.repository.TuningRequest
import com.example.motormates.notifications.AiNotifier
import com.example.motormates.notifications.AppForegroundState
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Las tareas de AI siguen sin pantalla y avisan solo cuando nadie las mira. */
class AiTaskManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: AiTuningRepository = mockk()
    private val notifier: AiNotifier = mockk(relaxed = true)
    private val foreground = AppForegroundState()
    private lateinit var manager: AiTaskManager

    private val vehicle = Vehicle("1", "Mazda", "MX-5", 2023, "Deportivo", "https://x/1.jpg", null, null, null, null)
    private val request = TuningRequest(vehicle, TuningMode.FREE, freeIdea = "franjas")
    private val first = GeneratedImage(TuningImage(byteArrayOf(1)), "p1", null)

    @Before
    fun setUp() {
        manager = AiTaskManager(
            repository, notifier, foreground,
            CoroutineScope(SupervisorJob() + mainDispatcherRule.testDispatcher)
        )
    }

    @Test
    fun `con la app cerrada muestra progreso y luego el resultado`() = runTest {
        val gate = CompletableDeferred<Result<GeneratedImage>>()
        coEvery { repository.generate(any()) } coAnswers { gate.await() }

        manager.startGeneration(request, emptyList(), TuningSummary())
        advanceUntilIdle()
        // Mientras trabaja, el servicio en primer plano mantiene viva la app.
        verify { notifier.keepAlive("Modificando tu Mazda MX-5…", any(), "aiTuning?vehicleId=1") }

        gate.complete(Result.success(first)); advanceUntilIdle()

        assertThat(manager.tuningTask.value?.status).isEqualTo(TaskStatus.DONE)
        verify { notifier.stopKeepAlive() }
        verify { notifier.showDone(AiTaskManager.TUNING_NOTIFICATION_ID, "¡Tu build está listo!", any(), "aiTuning?vehicleId=1", first.image.bytes) }
    }

    @Test
    fun `un ajuste cancelado conserva la imagen anterior`() = runTest {
        coEvery { repository.generate(any()) } returns Result.success(first)
        coEvery { repository.refine(any()) } coAnswers { CompletableDeferred<Result<GeneratedImage>>().await() }
        manager.startGeneration(request, emptyList(), TuningSummary()); advanceUntilIdle()

        manager.startRefine("más bajo"); advanceUntilIdle()
        assertThat(manager.tuningTask.value?.status).isEqualTo(TaskStatus.RUNNING)
        manager.cancelTuning()

        assertThat(manager.tuningTask.value?.status).isEqualTo(TaskStatus.DONE)
        assertThat(manager.tuningTask.value?.result).isEqualTo(first)
    }

    @Test
    fun `una generacion fallida queda con el error y avisa el fallo`() = runTest {
        coEvery { repository.generate(any()) } returns Result.failure(Exception("Sin red"))

        manager.startGeneration(request, emptyList(), TuningSummary()); advanceUntilIdle()

        assertThat(manager.tuningTask.value?.status).isEqualTo(TaskStatus.FAILED)
        assertThat(manager.tuningTask.value?.error).isEqualTo("Sin red")
        verify { notifier.showDone(AiTaskManager.TUNING_NOTIFICATION_ID, "No se pudo modificar tu Mazda MX-5", "Sin red", any(), null) }
    }

    @Test
    fun `una busqueda vista en pantalla no notifica`() = runTest {
        coEvery { repository.livePrice(any()) } returns Result.success(GroundedAnswer("ok"))
        foreground.onActivityStarted(mockk(relaxed = true))
        manager.setScreenVisible(AiTaskManager.SCREEN_FIND_PART, true)

        manager.startSearch("findPart", AiTaskManager.SCREEN_FIND_PART, "Buscando…", "Listo", "findPart") {
            Result.success(GroundedAnswer("Es un alerón"))
        }
        advanceUntilIdle()

        assertThat(manager.searchTasks.value["findPart"]?.answer?.text).isEqualTo("Es un alerón")
        verify(exactly = 0) { notifier.showDone(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `una busqueda sin nadie mirando notifica con el inicio de la respuesta`() = runTest {
        manager.startSearch("findPart", AiTaskManager.SCREEN_FIND_PART, "Buscando…", "Encontramos tu pieza", "findPart") {
            Result.success(GroundedAnswer("Es un alerón GT de fibra"))
        }
        advanceUntilIdle()

        verify {
            notifier.showDone(
                AiTaskManager.searchNotificationId("findPart"), "Encontramos tu pieza", "Es un alerón GT de fibra", "findPart", null
            )
        }
    }

    @Test
    fun `la pieza de encontrar pieza se entrega una sola vez`() {
        val image = TuningImage(byteArrayOf(7))
        manager.handOffPartImage(image)

        assertThat(manager.takeHandoffPartImage()).isEqualTo(image)
        assertThat(manager.takeHandoffPartImage()).isNull()
    }
}
