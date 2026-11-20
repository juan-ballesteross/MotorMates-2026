package com.example.motormates.ui.onboarding

import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.VEHICLE_CATEGORIES
import com.example.motormates.data.repository.CarModelOption
import com.example.motormates.data.util.SuggestedUser
import com.example.motormates.data.util.normalizedKey

/** Las preguntas, en orden. */
enum class OnboardingStep(val title: String, val subtitle: String) {
    CATEGORIES("¿Qué carros te mueven?", "Elige los que más te gustan. Con esto armamos tu feed."),
    BRANDS("Tus marcas favoritas", "Las que sigues, las que sueñas o las que manejas."),
    CAR("¿Tienes carro?", "Lo agregamos a tu garaje para que la comunidad lo vea."),
    INTERESTS("¿Qué buscas en MotorMates?", "Así destacamos lo que más te sirve."),
    CITY("¿Dónde ruedas?", "Para mostrarte lo que pasa cerca y centrar el mapa."),
    PEOPLE("Personas que te pueden interesar", "Síguelas y tu feed no arranca vacío.")
}

data class OnboardingState(
    val isLoading: Boolean = true,
    /** true: se abrió desde el perfil para editar; no hay "saltar" ni pantalla de personas. */
    val isEditMode: Boolean = false,
    val step: OnboardingStep = OnboardingStep.CATEGORIES,

    val categories: Set<String> = emptySet(),
    /** Foto de ejemplo de cada categoría, sacada del catálogo de vehículos. */
    val categoryImages: Map<String, String?> = emptyMap(),

    val brands: Set<String> = emptySet(),
    val brandOptions: List<String> = emptyList(),
    val brandQuery: String = "",

    /** null = todavía no contestó; false = no tiene carro. */
    val hasCar: Boolean? = null,
    val carBrand: String = "",
    val carModel: String = "",
    val carYear: String = "",
    val carCategory: String? = null,
    val carModelOptions: List<CarModelOption> = emptyList(),
    val isLoadingCarModels: Boolean = false,
    /** Ya tiene en su garaje un carro igual: no se vuelve a crear. */
    val carAlreadyInGarage: Boolean = false,

    val interests: Set<Interest> = emptySet(),

    val city: String? = null,
    val isLocating: Boolean = false,

    val suggestions: List<SuggestedUser> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val followed: Set<String> = emptySet(),

    /** Para validar el año del carro; lo pone el ViewModel con TimeSource. */
    val currentYear: Int = DEFAULT_CURRENT_YEAR,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Terminó (guardó, saltó o ya lo había hecho antes): la pantalla navega. */
    val isFinished: Boolean = false
) {
    val steps: List<OnboardingStep>
        get() = if (isEditMode) OnboardingStep.entries - OnboardingStep.PEOPLE else OnboardingStep.entries

    val stepIndex: Int get() = steps.indexOf(step).coerceAtLeast(0)
    val progress: Float get() = (stepIndex + 1f) / steps.size
    val isFirstStep: Boolean get() = stepIndex == 0
    val isLastStep: Boolean get() = stepIndex == steps.lastIndex

    val categoryOptions: List<String> get() = VEHICLE_CATEGORIES

    /** Las elegidas primero, luego las que coinciden con la búsqueda. */
    val visibleBrands: List<String>
        get() {
            val query = brandQuery.normalizedKey()
            val matching = brandOptions.filter { query.isEmpty() || it.normalizedKey().contains(query) }
            return brands.sortedBy { it.normalizedKey() } + matching.filterNot { it in brands }
        }

    val carYearNumber: Int? get() = carYear.toIntOrNull()?.takeIf { it in MIN_CAR_YEAR..currentYear + 1 }

    val isCarComplete: Boolean
        get() = carBrand.isNotBlank() && carModel.isNotBlank() && carYearNumber != null && carCategory != null

    /** Modelos que coinciden con lo escrito, para el autocompletado. */
    val visibleCarModels: List<CarModelOption>
        get() {
            val query = carModel.normalizedKey()
            return carModelOptions
                .filter { query.isEmpty() || it.name.normalizedKey().contains(query) }
                .filterNot { it.name.normalizedKey() == query }
                .take(MAX_MODEL_SUGGESTIONS)
        }

    /** El carro es lo único que, si se empieza, hay que terminar para seguir. */
    val canContinue: Boolean
        get() = !isSaving && when (step) {
            OnboardingStep.CAR -> hasCar != true || isCarComplete
            else -> true
        }

    companion object {
        const val MIN_CAR_YEAR = 1950
        const val DEFAULT_CURRENT_YEAR = 2026
        const val MAX_MODEL_SUGGESTIONS = 8
    }
}
