package com.example.motormates.ui.onboarding

import com.example.motormates.data.model.Interest
import com.example.motormates.data.model.VEHICLE_CATEGORIES
import com.example.motormates.data.util.SuggestedUser
import com.example.motormates.data.util.normalizedKey

/** Las preguntas, en orden. */
enum class OnboardingStep(val title: String, val subtitle: String) {
    CATEGORIES("¿Qué carros te mueven?", "Elige los que más te gustan. Con esto armamos tu feed."),
    BRANDS("Tus marcas favoritas", "Las que sigues, las que sueñas o las que manejas."),
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

    val interests: Set<Interest> = emptySet(),

    val city: String? = null,
    val isLocating: Boolean = false,

    val suggestions: List<SuggestedUser> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val followed: Set<String> = emptySet(),

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

    val canContinue: Boolean get() = !isSaving

}
