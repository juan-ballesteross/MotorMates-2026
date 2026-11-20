package com.example.motormates.data.model

/**
 * Lo que el usuario contó en el onboarding. Con esto se ordena el feed,
 * se sugieren personas y se centra el mapa.
 *
 * Kotlin puro (sin Android) para probar el ranking en la JVM.
 */
data class UserPreferences(
    /** Mismas etiquetas que Vehicle.category ("Deportivo", "SUV"…). */
    val categories: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val interests: List<Interest> = emptyList(),
    /** Una de CityCoordinates.CITIES, o null si no la dijo. */
    val city: String? = null,
    val car: CarRef? = null,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val updatedAt: Long = 0L
) {
    /** Ya pasó por el onboarding (lo terminó o lo saltó): no se le vuelve a mostrar solo. */
    val isDecided: Boolean get() = completed || skipped

    /** Hay algo con qué personalizar. */
    val hasTaste: Boolean get() = categories.isNotEmpty() || brands.isNotEmpty() || city != null
}

/** El carro del usuario, tal como lo eligió en el onboarding. */
data class CarRef(val brand: String, val model: String, val year: Int) {
    val displayName: String get() = "$brand $model"
}

/** Qué viene a buscar el usuario a la app. Decide qué funciones se destacan. */
enum class Interest(val label: String, val description: String) {
    BUY_RESEARCH("Reseñas antes de comprar", "Ver qué opina la gente de un carro"),
    MODIFY("Modificar mi carro", "Ideas, piezas y builds"),
    SOCIAL("Conocer gente y eventos", "Car meets, rodadas y amigos"),
    PHOTO("Fotografía", "Fotos de carros y spots");

    companion object {
        fun fromKey(key: String?): Interest? = entries.firstOrNull { it.name == key }
    }
}

/**
 * Tope de opciones por pregunta (y lo mismo exigen las reglas de Firestore):
 * marcar todo equivale a no decir nada y el ranking pierde sentido.
 */
const val MAX_PREFERENCE_SELECTION = 10
