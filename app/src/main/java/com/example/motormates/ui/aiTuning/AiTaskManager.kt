package com.example.motormates.ui.aiTuning

import com.example.motormates.data.injection.ApplicationScope
import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.data.repository.GeneratedImage
import com.example.motormates.data.repository.TuningRequest
import com.example.motormates.data.util.TuningPromptBuilder
import com.example.motormates.navigation.ScreenRoute
import com.example.motormates.notifications.AiNotifier
import com.example.motormates.notifications.AppForegroundState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskStatus { RUNNING, DONE, FAILED }

enum class TuningTaskKind { GENERATE, REFINE }

/**
 * La imagen de AI Tuning en curso o ya lista (una a la vez). Vive aquí y no
 * en el ViewModel para que siga aunque el usuario salga de la pantalla.
 * version cambia con cada novedad: la pantalla la usa para no repetir avisos.
 */
data class TuningTask(
    val version: Long,
    val vehicle: Vehicle,
    val kind: TuningTaskKind,
    val status: TaskStatus,
    val request: TuningRequest,
    /** Piezas y resumen con los que se pidió (para pintar el resultado). */
    val parts: List<Part>,
    val summary: TuningSummary,
    /** La última imagen lista. En un ajuste fallido se conserva la anterior. */
    val result: GeneratedImage? = null,
    val error: String? = null
)

/** Lo que el usuario lleva en "Encontrar pieza" (foto sin fondo y descripción). */
data class FindPartDraft(
    val image: TuningImage? = null,
    val description: String = ""
)

/** Una búsqueda con Google (precio en vivo, identificar o encontrar pieza). */
data class SearchTask(
    val key: String,
    val version: Long,
    val status: TaskStatus,
    val answer: GroundedAnswer? = null,
    val error: String? = null
)

/**
 * Corre las tareas de AI fuera de las pantallas (scope de aplicación) y
 * avisa con una notificación cuando terminan si el usuario no está mirando.
 *
 * Mientras una tarea corre, AiTaskService (servicio corto en primer plano)
 * mantiene viva la app aunque el usuario salga: sin él, Android congela el
 * proceso y la tarea se detiene. Si el sistema aun así mata el proceso, la
 * tarea se pierde (ver docs/ai-tuning.md).
 */
@Singleton
class AiTaskManager @Inject constructor(
    private val repository: AiTuningRepository,
    private val notifier: AiNotifier,
    private val foreground: AppForegroundState,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val nextVersion = AtomicLong(0)

    private val _tuningTask = MutableStateFlow<TuningTask?>(null)
    val tuningTask: StateFlow<TuningTask?> = _tuningTask.asStateFlow()
    private var tuningJob: Job? = null

    private val _searchTasks = MutableStateFlow<Map<String, SearchTask>>(emptyMap())
    val searchTasks: StateFlow<Map<String, SearchTask>> = _searchTasks.asStateFlow()
    private val searchJobs = ConcurrentHashMap<String, Job>()
    /** Texto y destino de la notificación de cada búsqueda. */
    private val searchInfo = ConcurrentHashMap<String, SearchInfo>()

    /** Pantallas abiertas ahora (SCREEN_TUNING, SCREEN_FIND_PART). */
    private val visibleScreens = MutableStateFlow<Set<String>>(emptySet())

    /** Imagen de una pieza que "Encontrar pieza" le pasa a AI Tuning. */
    private val _handoffPartImage = MutableStateFlow<TuningImage?>(null)
    val handoffPartImage: StateFlow<TuningImage?> = _handoffPartImage.asStateFlow()

    // ---------------- pantallas ----------------

    fun setScreenVisible(screen: String, visible: Boolean) {
        visibleScreens.update { if (visible) it + screen else it - screen }
    }

    private fun isWatching(screen: String): Boolean =
        foreground.isInForeground.value && screen in visibleScreens.value

    // ---------------- imagen ----------------

    fun startGeneration(request: TuningRequest, parts: List<Part>, summary: TuningSummary) {
        if (tuningJob?.isActive == true) return
        _tuningTask.value = TuningTask(
            version = version(),
            vehicle = request.vehicle,
            kind = TuningTaskKind.GENERATE,
            status = TaskStatus.RUNNING,
            request = request,
            parts = parts,
            summary = summary
        )
        keepAliveFor("Modificando tu ${TuningPromptBuilder.vehicleName(request.vehicle)}…", ScreenRoute.AiTuning.createRoute(request.vehicle.id))
        tuningJob = scope.launch {
            val result = runCatchingCancellable { repository.generate(request) }
            finishTuning(result)
        }
    }

    fun startRefine(instruction: String) {
        val task = _tuningTask.value ?: return
        if (tuningJob?.isActive == true || task.result == null) return
        _tuningTask.value = task.copy(
            version = version(), kind = TuningTaskKind.REFINE, status = TaskStatus.RUNNING, error = null
        )
        keepAliveFor("Ajustando tu ${TuningPromptBuilder.vehicleName(task.vehicle)}…", ScreenRoute.AiTuning.createRoute(task.vehicle.id))
        tuningJob = scope.launch {
            val result = runCatchingCancellable { repository.refine(instruction) }
            finishTuning(result)
        }
    }

    private fun finishTuning(result: Result<GeneratedImage>) {
        val task = _tuningTask.value ?: return
        val updated = result.fold(
            onSuccess = { task.copy(version = version(), status = TaskStatus.DONE, result = it, error = null) },
            onFailure = {
                task.copy(
                    version = version(),
                    status = TaskStatus.FAILED,
                    error = it.message ?: "No se pudo generar la imagen"
                )
            }
        )
        _tuningTask.value = updated
        releaseIfIdle()
        notifier.cancel(TUNING_NOTIFICATION_ID)
        if (isWatching(SCREEN_TUNING)) return

        val name = TuningPromptBuilder.vehicleName(updated.vehicle)
        val route = ScreenRoute.AiTuning.createRoute(updated.vehicle.id)
        if (updated.status == TaskStatus.DONE) {
            notifier.showDone(
                TUNING_NOTIFICATION_ID,
                title = if (updated.kind == TuningTaskKind.REFINE) "Tu ajuste está listo" else "¡Tu build está listo!",
                text = "Mira cómo quedó tu $name modificado",
                route = route,
                image = updated.result?.image?.bytes
            )
        } else {
            notifier.showDone(
                TUNING_NOTIFICATION_ID,
                title = "No se pudo modificar tu $name",
                text = updated.error.orEmpty(),
                route = route
            )
        }
    }

    fun cancelTuning() {
        tuningJob?.cancel()
        tuningJob = null
        notifier.cancel(TUNING_NOTIFICATION_ID)
        val task = _tuningTask.value
        // Un ajuste cancelado conserva la imagen anterior; una generación se descarta.
        _tuningTask.value = if (task != null && task.kind == TuningTaskKind.REFINE && task.result != null) {
            task.copy(version = version(), status = TaskStatus.DONE)
        } else {
            null
        }
        releaseIfIdle()
    }

    /** "Terminar": la próxima vez la pantalla arranca de cero. */
    fun clearTuning() {
        if (tuningJob?.isActive == true) return
        _tuningTask.value = null
        notifier.cancel(TUNING_NOTIFICATION_ID)
    }

    // ---------------- búsquedas con Google ----------------

    /**
     * key identifica la búsqueda (p. ej. "price:rin_18"); title y doneTitle
     * son los textos de la notificación; route, a dónde lleva al tocarla.
     */
    fun startSearch(
        key: String,
        screen: String,
        progressTitle: String,
        doneTitle: String,
        route: String,
        block: suspend () -> Result<GroundedAnswer>
    ) {
        if (searchJobs[key]?.isActive == true) return
        searchInfo[key] = SearchInfo(screen, progressTitle, doneTitle, route)
        updateSearch(key) { SearchTask(key, version(), TaskStatus.RUNNING) }
        keepAliveFor(progressTitle, route)
        searchJobs[key] = scope.launch {
            val result = runCatchingCancellable { block() }
            val info = searchInfo[key] ?: return@launch
            updateSearch(key) {
                result.fold(
                    onSuccess = { SearchTask(key, version(), TaskStatus.DONE, answer = it) },
                    onFailure = { SearchTask(key, version(), TaskStatus.FAILED, error = it.message ?: "No se pudo buscar") }
                )
            }
            releaseIfIdle()
            val id = searchNotificationId(key)
            notifier.cancel(id)
            if (!isWatching(info.screen)) {
                val task = _searchTasks.value[key]
                if (task?.status == TaskStatus.DONE) {
                    notifier.showDone(id, info.doneTitle, task.answer?.text.orEmpty().take(MAX_PREVIEW), info.route)
                } else {
                    notifier.showDone(id, "No se pudo completar la búsqueda", task?.error.orEmpty(), info.route)
                }
            }
        }
    }

    fun clearSearch(key: String) {
        searchJobs.remove(key)?.cancel()
        searchInfo.remove(key)
        notifier.cancel(searchNotificationId(key))
        _searchTasks.update { it - key }
        releaseIfIdle()
    }

    private fun updateSearch(key: String, block: () -> SearchTask) {
        _searchTasks.update { it + (key to block()) }
    }

    // ---------------- "Encontrar pieza" ----------------

    /**
     * La foto y la descripción de "Encontrar pieza" viven aquí: al volver
     * desde la notificación la pantalla se recrea y tiene que mostrar la
     * misma foto junto al resultado.
     */
    private val _findPartDraft = MutableStateFlow(FindPartDraft())
    val findPartDraft: StateFlow<FindPartDraft> = _findPartDraft.asStateFlow()

    fun updateFindPartDraft(transform: (FindPartDraft) -> FindPartDraft) = _findPartDraft.update(transform)

    // ---------------- "Encontrar pieza" → AI Tuning ----------------

    fun handOffPartImage(image: TuningImage) {
        _handoffPartImage.value = image
    }

    /** La toma AI Tuning una vez (para no volver a cargarla en otra visita). */
    fun takeHandoffPartImage(): TuningImage? = _handoffPartImage.value.also { _handoffPartImage.value = null }

    // ---------------- servicio "trabajando" ----------------

    /**
     * Mientras algo corre, un servicio en primer plano evita que Android
     * congele la app al salir (si no, la tarea se detiene hasta volver).
     */
    private fun keepAliveFor(title: String, route: String) {
        notifier.keepAlive(title, "Te avisamos cuando termine. Puedes seguir usando la app.", route)
    }

    private fun releaseIfIdle() {
        val tuningRunning = _tuningTask.value?.status == TaskStatus.RUNNING
        val searchRunning = _searchTasks.value.values.any { it.status == TaskStatus.RUNNING }
        if (!tuningRunning && !searchRunning) notifier.stopKeepAlive()
    }

    private fun version(): Long = nextVersion.incrementAndGet()

    private suspend fun <T> runCatchingCancellable(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private data class SearchInfo(
        val screen: String,
        val progressTitle: String,
        val doneTitle: String,
        val route: String
    )

    companion object {
        const val SCREEN_TUNING = "screen:aiTuning"
        const val SCREEN_FIND_PART = "screen:findPart"

        const val TUNING_NOTIFICATION_ID = 2001
        private const val SEARCH_NOTIFICATION_BASE = 3000
        private const val MAX_PREVIEW = 140

        fun priceKey(partId: String) = "price:$partId"
        const val IDENTIFY_KEY = "identify:tuning"
        const val FIND_PART_KEY = "findPart"

        internal fun searchNotificationId(key: String) = SEARCH_NOTIFICATION_BASE + (key.hashCode() and 0x3FF)
    }
}
