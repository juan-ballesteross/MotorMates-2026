package com.example.motormates.ui.aiTuning

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.BuildDraft
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.BuildRepository
import com.example.motormates.data.repository.PartsCatalogRepository
import com.example.motormates.data.repository.TuningRequest
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.util.SpecCalculator
import com.example.motormates.data.util.SpecParser
import com.example.motormates.data.util.TuningPromptBuilder
import com.example.motormates.navigation.ScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Configurador de AI Tuning. Precio y rendimiento se recalculan al instante
 * con cada toque (SpecCalculator, sin red). Lo que usa la AI (imagen,
 * ajustes, precios con Google) corre en AiTaskManager: sigue aunque el
 * usuario salga de la pantalla, y al volver el resultado sigue aquí.
 */
@HiltViewModel
class AiTuningViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val catalogRepository: PartsCatalogRepository,
    private val aiTuningRepository: AiTuningRepository,
    private val buildRepository: BuildRepository,
    private val authRepository: AuthRepository,
    private val taskManager: AiTaskManager
) : ViewModel() {

    private val _state = MutableStateFlow(AiTuningState())
    val state: StateFlow<AiTuningState> = _state.asStateFlow()

    private var loadedVehicleId: String? = null
    private var started = false
    /** Última versión de la tarea ya pintada, para no repetir avisos. */
    private var appliedTaskVersion = 0L
    /**
     * Si la imagen termina con el usuario fuera de la app, la ficha técnica
     * se pide al volver: en segundo plano Android corta la red y fallaría.
     */
    private var screenVisible = false
    private var insightsPending = false

    init {
        viewModelScope.launch { taskManager.tuningTask.collect(::applyTuningTask) }
        viewModelScope.launch { taskManager.searchTasks.collect(::applySearchTasks) }
    }

    /**
     * vehicleId null: se entró desde "+" o desde el feed y hay que elegir
     * carro. Si hay una modificación en curso o lista, se retoma ese carro.
     */
    fun start(vehicleId: String?) {
        if (started) return
        started = true
        val resumeId = vehicleId ?: taskManager.tuningTask.value?.vehicle?.id
        _state.update { it.copy(canChangeVehicle = vehicleId == null) }
        if (resumeId != null) load(resumeId) else openVehiclePicker()
    }

    fun setScreenVisible(visible: Boolean) {
        screenVisible = visible
        taskManager.setScreenVisible(AiTaskManager.SCREEN_TUNING, visible)
        if (visible && insightsPending) {
            insightsPending = false
            loadInsights()
        }
    }

    override fun onCleared() {
        taskManager.setScreenVisible(AiTaskManager.SCREEN_TUNING, false)
    }

    // ---------------- elegir vehículo ----------------

    fun openVehiclePicker() {
        _state.update { it.copy(phase = TuningPhase.PICK_VEHICLE, isLoading = false, loadError = null) }
        if (_state.value.pickerVehicles.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isPickerLoading = true, pickerError = null) }
            vehicleRepository.getVehicles().fold(
                onSuccess = { vehicles ->
                    _state.update {
                        it.copy(
                            isPickerLoading = false,
                            pickerVehicles = vehicles,
                            currentUserId = authRepository.getCurrentUser()?.id
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isPickerLoading = false, pickerError = error.message ?: "No se pudieron cargar los carros") }
                }
            )
        }
    }

    fun updatePickerQuery(query: String) = _state.update { it.copy(pickerQuery = query.take(MAX_QUERY)) }

    fun selectPickerCategory(category: String?) = _state.update {
        it.copy(pickerCategory = if (it.pickerCategory == category) null else category)
    }

    fun pickVehicle(vehicleId: String) {
        val busy = taskManager.tuningTask.value
        // Cambiar de carro descarta un resultado sin guardar del anterior.
        if (busy != null && busy.vehicle.id != vehicleId && busy.status != TaskStatus.RUNNING) {
            taskManager.clearTuning()
        }
        appliedTaskVersion = 0
        _state.update { it.resetForVehicle() }
        loadedVehicleId = null
        load(vehicleId)
    }

    // ---------------- carga ----------------

    fun load(vehicleId: String) {
        if (loadedVehicleId == vehicleId && _state.value.vehicle != null) return
        loadedVehicleId = vehicleId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null, phase = TuningPhase.CONFIGURING) }

            val vehicle = vehicleRepository.getVehicleById(vehicleId).getOrElse { error ->
                _state.update { it.copy(isLoading = false, loadError = error.message ?: "No se pudo cargar el vehículo") }
                return@launch
            }
            val catalog = catalogRepository.getCatalog().getOrElse { error ->
                _state.update { it.copy(isLoading = false, loadError = error.message ?: "No se pudo cargar el catálogo") }
                return@launch
            }.compatibleWith(vehicle.category)

            val currentUserId = authRepository.getCurrentUser()?.id
            val categories = PartCategory.entries.filter { c -> catalog.parts.any { it.category == c } }
            // Una pieza que llega desde "Encontrar pieza" abre directo en "Tu pieza".
            val handoff = taskManager.takeHandoffPartImage()
            _state.update {
                it.copy(
                    isLoading = false,
                    vehicle = vehicle,
                    currentUserId = currentUserId,
                    isOwner = vehicle.ownerId != null && vehicle.ownerId == currentUserId,
                    parts = catalog.parts,
                    packages = catalog.packages,
                    selectedCategory = categories.firstOrNull() ?: PartCategory.COLOR,
                    summary = SpecCalculator.calculate(SpecParser.specsOf(vehicle), emptyList()),
                    mode = if (handoff != null) TuningMode.OWN_PART else it.mode,
                    ownPartImage = handoff ?: it.ownPartImage,
                    loadError = if (vehicle.imageUrl == null) {
                        "Este vehículo no tiene foto: AI Tuning necesita una foto para modificarla."
                    } else null
                ).withBusyVehicle(taskManager.tuningTask.value)
            }
            // Si ya había algo en curso o listo para este carro, se retoma.
            appliedTaskVersion = 0
            applyTuningTask(taskManager.tuningTask.value)
            applySearchTasks(taskManager.searchTasks.value)
            refreshRemainingGenerations()
        }
    }

    // ---------------- configurador ----------------

    fun selectMode(mode: TuningMode) = _state.update { it.copy(mode = mode) }

    fun selectCategory(category: PartCategory) = _state.update { it.copy(selectedCategory = category) }

    /** Una pieza por categoría: tocar otra la reemplaza; tocar la misma la quita. */
    fun togglePart(partId: String) {
        val part = _state.value.parts.firstOrNull { it.id == partId } ?: return
        _state.update { state ->
            val current = state.selection[part.category]
            val selection = if (current == partId) {
                state.selection - part.category
            } else {
                state.selection + (part.category to partId)
            }
            state.withSelection(selection, activePackageId = null)
        }
    }

    /**
     * Un paquete trae piezas de varias categorías; si trae dos de la misma
     * (p. ej. dos rines para distintos tipos de carro), gana la primera
     * compatible. Tocar el paquete activo lo quita.
     */
    fun applyPackage(packageId: String) {
        val state = _state.value
        if (state.activePackageId == packageId) {
            clearSelection()
            return
        }
        val pkg = state.packages.firstOrNull { it.id == packageId } ?: return
        val selection = mutableMapOf<PartCategory, String>()
        pkg.partIds
            .mapNotNull { id -> state.parts.firstOrNull { it.id == id } }
            .forEach { part -> selection.putIfAbsent(part.category, part.id) }
        _state.update { it.withSelection(selection, activePackageId = packageId).copy(mode = TuningMode.CATALOG) }
    }

    fun clearSelection() = _state.update { it.withSelection(emptyMap(), activePackageId = null) }

    fun updateFreeIdea(text: String) = _state.update { it.copy(freeIdea = text.take(MAX_TEXT)) }

    fun updateOwnPartDescription(text: String) =
        _state.update { it.copy(ownPartDescription = text.take(MAX_TEXT)) }

    /** Foto de la pieza elegida o tomada: se le quita el fondo en el dispositivo. */
    fun onOwnPartPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isPreparingOwnPart = true) }
            taskManager.clearSearch(AiTaskManager.IDENTIFY_KEY)
            aiTuningRepository.prepareOwnPart(uri)
                .onSuccess { image -> _state.update { it.copy(isPreparingOwnPart = false, ownPartImage = image) } }
                .onFailure { error ->
                    _state.update { it.copy(isPreparingOwnPart = false, message = error.message) }
                }
        }
    }

    fun removeOwnPart() {
        taskManager.clearSearch(AiTaskManager.IDENTIFY_KEY)
        _state.update { it.copy(ownPartImage = null, ownPartIdentification = null) }
    }

    /** "¿Qué pieza es y cuánto cuesta?" con Google. Sigue aunque salga de la pantalla. */
    fun identifyOwnPart() {
        val image = _state.value.ownPartImage ?: return
        val description = _state.value.ownPartDescription
        taskManager.startSearch(
            key = AiTaskManager.IDENTIFY_KEY,
            screen = AiTaskManager.SCREEN_TUNING,
            progressTitle = "Identificando tu pieza…",
            doneTitle = "Encontramos tu pieza",
            route = currentRoute()
        ) { aiTuningRepository.identifyPart(image, description) }
    }

    // ---------------- generación ----------------

    fun generate() {
        val state = _state.value
        val vehicle = state.vehicle ?: return
        if (!state.canGenerate) return

        val parts = if (state.mode == TuningMode.CATALOG) state.selectedParts else emptyList()
        val request = TuningRequest(
            vehicle = vehicle,
            mode = state.mode,
            parts = parts,
            ownPartImage = state.ownPartImage,
            ownPartDescription = state.ownPartDescription,
            freeIdea = state.freeIdea
        )
        _state.update { it.copy(insights = null, insightsError = null, savedBuild = null) }
        taskManager.startGeneration(request, parts, state.summary)
    }

    fun cancelGeneration() {
        taskManager.cancelTuning()
        _state.update { it.copy(phase = TuningPhase.CONFIGURING, isRefining = false) }
    }

    /** La ficha técnica es independiente de la imagen: si falla, se puede reintentar. */
    fun loadInsights() {
        val state = _state.value
        val vehicle = state.vehicle ?: return
        if (state.resultParts.isEmpty() || state.isInsightsLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isInsightsLoading = true, insightsError = null) }
            aiTuningRepository.explain(vehicle, state.resultParts, state.resultSummary)
                .onSuccess { insights -> _state.update { it.copy(isInsightsLoading = false, insights = insights) } }
                .onFailure { error ->
                    _state.update { it.copy(isInsightsLoading = false, insightsError = error.message) }
                }
        }
    }

    fun updateRefineText(text: String) = _state.update { it.copy(refineText = text.take(MAX_TEXT)) }

    /** Ajuste conversacional sobre la imagen actual. */
    fun refine() {
        val instruction = _state.value.refineText.trim()
        if (instruction.isEmpty() || _state.value.isRefining) return
        _state.update { it.copy(refineText = "") }
        taskManager.startRefine(instruction)
    }

    fun backToConfigurator() = _state.update { it.copy(phase = TuningPhase.CONFIGURING) }

    fun requestLivePrice(partId: String) {
        val part = _state.value.resultParts.firstOrNull { it.id == partId }
            ?: _state.value.parts.firstOrNull { it.id == partId }
            ?: return
        taskManager.startSearch(
            key = AiTaskManager.priceKey(partId),
            screen = AiTaskManager.SCREEN_TUNING,
            progressTitle = "Buscando el precio de ${part.name}…",
            doneTitle = "Precio actual de ${part.name}",
            route = currentRoute()
        ) { aiTuningRepository.livePrice(part) }
    }

    // ---------------- terminar ----------------

    /**
     * "Terminar": si el resultado no se ha guardado, primero pregunta (no
     * se pierde un build por un toque).
     */
    fun requestFinish() {
        val state = _state.value
        val unsaved = state.phase == TuningPhase.RESULT && state.savedBuild == null && state.resultImage != null
        if (unsaved) _state.update { it.copy(showFinishConfirm = true) } else finish()
    }

    fun dismissFinishConfirm() = _state.update { it.copy(showFinishConfirm = false) }

    fun finish() {
        taskManager.clearTuning()
        taskManager.clearSearch(AiTaskManager.IDENTIFY_KEY)
        _state.value.livePrices.keys.forEach { taskManager.clearSearch(AiTaskManager.priceKey(it)) }
        _state.update { it.copy(showFinishConfirm = false, isFinished = true) }
    }

    // ---------------- guardar ----------------

    fun save(publish: Boolean) {
        val state = _state.value
        val vehicle = state.vehicle ?: return
        val image = state.resultImage ?: return
        if (state.isSaving) return

        val draft = BuildDraft(
            vehicleId = vehicle.id,
            vehicleName = TuningPromptBuilder.vehicleName(vehicle),
            baseImageUrl = vehicle.imageUrl,
            parts = state.resultParts,
            mode = state.resultMode,
            // Se guarda lo que escribió el usuario, no el prompt técnico.
            prompt = when (state.resultMode) {
                TuningMode.FREE -> state.freeIdea
                TuningMode.OWN_PART -> state.ownPartDescription.ifBlank { null }
                TuningMode.CATALOG -> null
            },
            summary = state.resultSummary,
            insights = state.insights
        )
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            buildRepository.save(draft, image, publish)
                .onSuccess { build ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            savedBuild = build,
                            message = if (publish) "Build publicado en la comunidad" else "Build guardado en tu perfil"
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isSaving = false, message = error.message) } }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // ---------------- tareas en segundo plano → estado ----------------

    private fun applyTuningTask(task: TuningTask?) {
        val vehicleId = _state.value.vehicle?.id
        _state.update { it.withBusyVehicle(task) }
        if (task == null) {
            if (_state.value.phase == TuningPhase.GENERATING) {
                _state.update { it.copy(phase = TuningPhase.CONFIGURING) }
            }
            _state.update { it.copy(isRefining = false) }
            return
        }
        if (vehicleId == null || task.vehicle.id != vehicleId || task.version <= appliedTaskVersion) return
        appliedTaskVersion = task.version

        when (task.status) {
            TaskStatus.RUNNING -> _state.update {
                if (task.kind == TuningTaskKind.REFINE) {
                    it.withResult(task).copy(phase = TuningPhase.RESULT, isRefining = true)
                } else {
                    it.copy(phase = TuningPhase.GENERATING, isRefining = false)
                }
            }
            TaskStatus.DONE -> {
                val imageChanged = _state.value.resultImage != task.result?.image
                _state.update {
                    it.withResult(task).copy(
                        phase = TuningPhase.RESULT,
                        isRefining = false,
                        savedBuild = if (imageChanged) null else it.savedBuild,
                        remainingGenerations = task.result?.remainingToday ?: it.remainingGenerations
                    )
                }
                if (task.request.mode == TuningMode.CATALOG && _state.value.insights == null) {
                    if (screenVisible) loadInsights() else insightsPending = true
                }
            }
            TaskStatus.FAILED -> {
                if (task.kind == TuningTaskKind.REFINE && task.result != null) {
                    _state.update { it.withResult(task).copy(phase = TuningPhase.RESULT, isRefining = false, message = task.error) }
                } else {
                    _state.update { it.copy(phase = TuningPhase.CONFIGURING, isRefining = false, message = task.error) }
                    taskManager.clearTuning()
                }
                refreshRemainingGenerations()
            }
        }
    }

    private fun applySearchTasks(tasks: Map<String, SearchTask>) {
        _state.update { state ->
            val prices = tasks.filterKeys { it.startsWith(PRICE_PREFIX) }
                .map { (key, task) -> key.removePrefix(PRICE_PREFIX) to task.toUi() }
                .toMap()
            state.copy(
                livePrices = prices,
                ownPartIdentification = tasks[AiTaskManager.IDENTIFY_KEY]?.toUi()
            )
        }
    }

    private fun SearchTask.toUi() = GroundedUi(
        isLoading = status == TaskStatus.RUNNING,
        answer = answer,
        error = error
    )

    private fun AiTuningState.withResult(task: TuningTask): AiTuningState {
        val result = task.result ?: return this
        // Al volver de una notificación, la selección se reconstruye con lo pedido.
        val selection = if (selection.isEmpty()) task.parts.associate { it.category to it.id } else selection
        return copy(
            resultImage = result.image,
            resultMode = task.request.mode,
            resultParts = task.parts,
            resultSummary = task.summary,
            lastPrompt = result.prompt,
            mode = task.request.mode,
            selection = selection,
            freeIdea = freeIdea.ifBlank { task.request.freeIdea },
            ownPartImage = ownPartImage ?: task.request.ownPartImage,
            ownPartDescription = ownPartDescription.ifBlank { task.request.ownPartDescription }
        )
    }

    /** Una modificación de OTRO carro en curso bloquea generar aquí (una a la vez). */
    private fun AiTuningState.withBusyVehicle(task: TuningTask?): AiTuningState = copy(
        busyWithVehicleName = task
            ?.takeIf { it.status == TaskStatus.RUNNING && it.vehicle.id != vehicle?.id }
            ?.let { TuningPromptBuilder.vehicleName(it.vehicle) }
    )

    private fun AiTuningState.resetForVehicle(): AiTuningState = AiTuningState(
        canChangeVehicle = canChangeVehicle,
        pickerVehicles = pickerVehicles,
        currentUserId = currentUserId,
        isLoading = true
    )

    private fun currentRoute(): String = ScreenRoute.AiTuning.createRoute(_state.value.vehicle?.id)

    private fun refreshRemainingGenerations() {
        viewModelScope.launch {
            val remaining = aiTuningRepository.remainingGenerations()
            _state.update { it.copy(remainingGenerations = remaining) }
        }
    }

    private fun AiTuningState.withSelection(
        selection: Map<PartCategory, String>,
        activePackageId: String?
    ): AiTuningState {
        val vehicle = vehicle ?: return copy(selection = selection, activePackageId = activePackageId)
        val selected = PartCategory.entries.mapNotNull { category ->
            selection[category]?.let { id -> parts.firstOrNull { it.id == id } }
        }
        return copy(
            selection = selection,
            activePackageId = activePackageId,
            summary = SpecCalculator.calculate(SpecParser.specsOf(vehicle), selected)
        )
    }

    private companion object {
        const val MAX_TEXT = 300
        const val MAX_QUERY = 60
        const val PRICE_PREFIX = "price:"
    }
}

/** Filtro del selector de carros: texto libre y categoría. */
internal fun List<Vehicle>.filterForPicker(query: String, category: String?): List<Vehicle> {
    val q = query.trim()
    return filter { vehicle ->
        (category == null || vehicle.category.equals(category, ignoreCase = true)) &&
            (q.isEmpty() || "${vehicle.brand} ${vehicle.model} ${vehicle.year}".contains(q, ignoreCase = true))
    }
}
