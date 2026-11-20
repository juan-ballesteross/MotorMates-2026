package com.example.motormates.ui.aiTuning

import com.example.motormates.data.model.Build
import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningPackage
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle

/**
 * PICK_VEHICLE: eligiendo qué carro modificar (se entró sin carro).
 * CONFIGURING: eligiendo piezas. GENERATING: esperando a la AI.
 * RESULT: antes/después.
 */
enum class TuningPhase { PICK_VEHICLE, CONFIGURING, GENERATING, RESULT }

/** Estado de una búsqueda con Google (precio en vivo o identificación de pieza). */
data class GroundedUi(
    val isLoading: Boolean = false,
    val answer: GroundedAnswer? = null,
    val error: String? = null
)

data class AiTuningState(
    // --- carga ---
    val isLoading: Boolean = true,
    /** Error que impide mostrar la pantalla (vehículo o catálogo). */
    val loadError: String? = null,
    /** Aviso pasajero (snackbar). */
    val message: String? = null,
    val vehicle: Vehicle? = null,
    val isOwner: Boolean = false,
    val currentUserId: String? = null,

    // --- elegir vehículo (cuando se entra sin carro) ---
    /** Se entró sin carro: se puede volver a elegir otro desde el configurador. */
    val canChangeVehicle: Boolean = false,
    val pickerVehicles: List<Vehicle> = emptyList(),
    val pickerQuery: String = "",
    val pickerCategory: String? = null,
    val isPickerLoading: Boolean = false,
    val pickerError: String? = null,

    // --- configurador ---
    /** Solo las piezas compatibles con este vehículo. */
    val parts: List<Part> = emptyList(),
    val packages: List<TuningPackage> = emptyList(),
    val mode: TuningMode = TuningMode.CATALOG,
    val selectedCategory: PartCategory = PartCategory.COLOR,
    /** Una pieza por categoría: categoría → id de la pieza. */
    val selection: Map<PartCategory, String> = emptyMap(),
    val activePackageId: String? = null,
    /** Precio y rendimiento en vivo, sin AI. */
    val summary: TuningSummary = TuningSummary(),
    val freeIdea: String = "",
    val ownPartImage: TuningImage? = null,
    val ownPartDescription: String = "",
    val isPreparingOwnPart: Boolean = false,
    val ownPartIdentification: GroundedUi? = null,

    // --- generación y resultado ---
    val phase: TuningPhase = TuningPhase.CONFIGURING,
    val resultImage: TuningImage? = null,
    /** Modo y piezas con los que se generó el resultado actual. */
    val resultMode: TuningMode = TuningMode.CATALOG,
    val resultParts: List<Part> = emptyList(),
    val resultSummary: TuningSummary = TuningSummary(),
    val lastPrompt: String? = null,
    val insights: TuningInsights? = null,
    val isInsightsLoading: Boolean = false,
    val insightsError: String? = null,
    val refineText: String = "",
    val isRefining: Boolean = false,
    /** Precio en vivo por id de pieza. */
    val livePrices: Map<String, GroundedUi> = emptyMap(),
    val remainingGenerations: Int? = null,
    val isSaving: Boolean = false,
    val savedBuild: Build? = null,

    // --- terminar ---
    /** "¿Salir sin guardar?" al tocar Terminar con un resultado sin guardar. */
    val showFinishConfirm: Boolean = false,
    /** Terminó: la pantalla vuelve al inicio. */
    val isFinished: Boolean = false,
    /** Otro carro se está modificando ahora (una modificación a la vez). */
    val busyWithVehicleName: String? = null
) {
    /** Carros del usuario primero ("Mis carros"), con el filtro aplicado. */
    val myPickerVehicles: List<Vehicle>
        get() = pickerVehicles.filter { currentUserId != null && it.ownerId == currentUserId }
            .filterForPicker(pickerQuery, pickerCategory)

    val otherPickerVehicles: List<Vehicle>
        get() = pickerVehicles.filterNot { currentUserId != null && it.ownerId == currentUserId }
            .filterForPicker(pickerQuery, pickerCategory)

    val pickerCategories: List<String>
        get() = pickerVehicles.map { it.category }.distinct().sorted()

    /** En el orden de los chips, no en el orden en que se tocaron. */
    val selectedParts: List<Part>
        get() = PartCategory.entries.mapNotNull { category ->
            selection[category]?.let { id -> parts.firstOrNull { it.id == id } }
        }

    /** Categorías que tienen al menos una pieza para este vehículo. */
    val categories: List<PartCategory>
        get() = PartCategory.entries.filter { category -> parts.any { it.category == category } }

    val partsInSelectedCategory: List<Part>
        get() = parts.filter { it.category == selectedCategory }

    val canGenerate: Boolean
        get() = vehicle?.imageUrl != null && !isPreparingOwnPart && when (mode) {
            TuningMode.CATALOG -> selection.isNotEmpty()
            TuningMode.OWN_PART -> ownPartImage != null
            TuningMode.FREE -> freeIdea.isNotBlank()
        } && remainingGenerations != 0 && busyWithVehicleName == null

    val hasNoGenerationsLeft: Boolean get() = remainingGenerations == 0
}
