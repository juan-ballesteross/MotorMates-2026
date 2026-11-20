package com.example.motormates.ui.aiTuning

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.BuildDraft
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.repository.AiTuningRepository
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.BuildRepository
import com.example.motormates.data.repository.PartsCatalogRepository
import com.example.motormates.data.repository.TuningRequest
import com.example.motormates.data.repository.VehicleRepository
import com.example.motormates.data.util.SpecCalculator
import com.example.motormates.data.util.SpecParser
import com.example.motormates.data.util.TuningPromptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Configurador de AI Tuning. Precio y rendimiento se recalculan al instante
 * con cada toque (SpecCalculator, sin red); la AI solo entra al generar la
 * imagen, al redactar la ficha y al buscar precios en vivo.
 */
@HiltViewModel
class AiTuningViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val catalogRepository: PartsCatalogRepository,
    private val aiTuningRepository: AiTuningRepository,
    private val buildRepository: BuildRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AiTuningState())
    val state: StateFlow<AiTuningState> = _state.asStateFlow()

    private var loadedVehicleId: String? = null
    private var generationJob: Job? = null

    fun load(vehicleId: String) {
        if (loadedVehicleId == vehicleId && _state.value.vehicle != null) return
        loadedVehicleId = vehicleId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }

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
            _state.update {
                it.copy(
                    isLoading = false,
                    vehicle = vehicle,
                    isOwner = vehicle.ownerId != null && vehicle.ownerId == currentUserId,
                    parts = catalog.parts,
                    packages = catalog.packages,
                    selectedCategory = categories.firstOrNull() ?: PartCategory.COLOR,
                    summary = SpecCalculator.calculate(SpecParser.specsOf(vehicle), emptyList()),
                    loadError = if (vehicle.imageUrl == null) {
                        "Este vehículo no tiene foto: AI Tuning necesita una foto para modificarla."
                    } else null
                )
            }
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
            _state.update { it.copy(isPreparingOwnPart = true, ownPartIdentification = null) }
            aiTuningRepository.prepareOwnPart(uri)
                .onSuccess { image -> _state.update { it.copy(isPreparingOwnPart = false, ownPartImage = image) } }
                .onFailure { error ->
                    _state.update { it.copy(isPreparingOwnPart = false, message = error.message) }
                }
        }
    }

    fun removeOwnPart() = _state.update { it.copy(ownPartImage = null, ownPartIdentification = null) }

    /** "¿Qué pieza es y cuánto cuesta?" con búsqueda de Google. */
    fun identifyOwnPart() {
        val image = _state.value.ownPartImage ?: return
        viewModelScope.launch {
            _state.update { it.copy(ownPartIdentification = GroundedUi(isLoading = true)) }
            val result = aiTuningRepository.identifyPart(image, _state.value.ownPartDescription)
            _state.update {
                it.copy(
                    ownPartIdentification = result.fold(
                        onSuccess = { answer -> GroundedUi(answer = answer) },
                        onFailure = { error -> GroundedUi(error = error.message) }
                    )
                )
            }
        }
    }

    // ---------------- generación ----------------

    fun generate() {
        val state = _state.value
        val vehicle = state.vehicle ?: return
        if (!state.canGenerate || generationJob?.isActive == true) return

        val parts = if (state.mode == TuningMode.CATALOG) state.selectedParts else emptyList()
        val summary = state.summary
        val request = TuningRequest(
            vehicle = vehicle,
            mode = state.mode,
            parts = parts,
            ownPartImage = state.ownPartImage,
            ownPartDescription = state.ownPartDescription,
            freeIdea = state.freeIdea
        )

        generationJob = viewModelScope.launch {
            _state.update { it.copy(phase = TuningPhase.GENERATING) }
            aiTuningRepository.generate(request)
                .onSuccess { generated ->
                    _state.update {
                        it.copy(
                            phase = TuningPhase.RESULT,
                            resultImage = generated.image,
                            resultMode = request.mode,
                            resultParts = parts,
                            resultSummary = summary,
                            lastPrompt = generated.prompt,
                            remainingGenerations = generated.remainingToday,
                            insights = null,
                            insightsError = null,
                            refineText = "",
                            livePrices = emptyMap(),
                            savedBuild = null
                        )
                    }
                    if (request.mode == TuningMode.CATALOG) loadInsights()
                }
                .onFailure { error ->
                    _state.update { it.copy(phase = TuningPhase.CONFIGURING, message = error.message) }
                    refreshRemainingGenerations()
                }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _state.update { it.copy(phase = TuningPhase.CONFIGURING) }
    }

    /** La ficha técnica es independiente de la imagen: si falla, se puede reintentar. */
    fun loadInsights() {
        val state = _state.value
        val vehicle = state.vehicle ?: return
        if (state.resultParts.isEmpty()) return
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
        viewModelScope.launch {
            _state.update { it.copy(isRefining = true) }
            aiTuningRepository.refine(instruction)
                .onSuccess { generated ->
                    _state.update {
                        it.copy(
                            isRefining = false,
                            resultImage = generated.image,
                            lastPrompt = generated.prompt,
                            remainingGenerations = generated.remainingToday,
                            refineText = "",
                            savedBuild = null
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isRefining = false, message = error.message) } }
        }
    }

    fun backToConfigurator() = _state.update { it.copy(phase = TuningPhase.CONFIGURING) }

    fun requestLivePrice(partId: String) {
        val part = _state.value.resultParts.firstOrNull { it.id == partId }
            ?: _state.value.parts.firstOrNull { it.id == partId }
            ?: return
        if (_state.value.livePrices[partId]?.isLoading == true) return
        viewModelScope.launch {
            _state.update { it.copy(livePrices = it.livePrices + (partId to GroundedUi(isLoading = true))) }
            val result = aiTuningRepository.livePrice(part)
            val ui = result.fold(
                onSuccess = { GroundedUi(answer = it) },
                onFailure = { GroundedUi(error = it.message) }
            )
            _state.update { it.copy(livePrices = it.livePrices + (partId to ui)) }
        }
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
    }
}
