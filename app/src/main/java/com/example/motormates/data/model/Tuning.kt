package com.example.motormates.data.model

import java.text.Normalizer

/**
 * Modelos de dominio de AI Tuning. Principio de diseño de la funcionalidad:
 * la AI dibuja y explica, el código calcula y el catálogo garantiza. Por eso
 * precio y rendimiento viven aquí (datos del catálogo) y no en la respuesta
 * del modelo.
 */

/** Categorías del configurador, en el orden en que se muestran los chips. */
enum class PartCategory(val label: String) {
    COLOR("Color"),
    RINES("Rines"),
    AERO("Aero"),
    SUSPENSION("Suspensión"),
    ESCAPE("Escape"),
    ADMISION("Admisión"),
    ECU("Motor"),
    LUCES("Luces y vidrios");

    companion object {
        fun fromKey(key: String?): PartCategory? = entries.firstOrNull { it.name == key }
    }
}

/**
 * Una pieza real del catálogo. Los precios son un rango en COP: es más
 * honesto que un precio exacto y aguanta el paso del tiempo.
 *
 * @property compatibleCategories categorías de vehículo ("Deportivo", "SUV"…)
 *   en las que se ofrece. Vacía = todas.
 * @property visible false para piezas que no se ven por fuera (filtro, ECU):
 *   cuentan en precio y rendimiento pero no se le pide a la AI que las dibuje.
 * @property visualPrompt descripción en inglés de cómo se ve instalada; es lo
 *   que se le manda al modelo de imagen.
 */
data class Part(
    val id: String,
    val name: String,
    val brand: String,
    val category: PartCategory,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val priceMinCop: Long,
    val priceMaxCop: Long,
    val priceSource: String = PRICE_SOURCE_ESTIMATED,
    val priceCheckedAt: String? = null,
    val hpDeltaPct: Double = 0.0,
    val torqueDeltaPct: Double = 0.0,
    val weightDeltaKg: Double = 0.0,
    val heightDeltaMm: Int = 0,
    val compatibleCategories: List<String> = emptyList(),
    val visible: Boolean = true,
    val visualPrompt: String = "",
    val perfSource: String? = null,
    val colorHex: String? = null,
    val finish: String? = null
) {
    val isPriceEstimated: Boolean get() = priceSource == PRICE_SOURCE_ESTIMATED

    /** Compara sin tildes ni mayúsculas: "Clásico" == "clasico". */
    fun isCompatibleWith(vehicleCategory: String): Boolean =
        compatibleCategories.isEmpty() ||
            compatibleCategories.any { it.normalizedKey() == vehicleCategory.normalizedKey() }

    companion object {
        const val PRICE_SOURCE_ESTIMATED = "estimado"
    }
}

/** Combinación de un toque (Street, Track, Show, Off-road). */
data class TuningPackage(
    val id: String,
    val name: String,
    val description: String,
    val partIds: List<String>
)

data class PartsCatalog(
    val parts: List<Part>,
    val packages: List<TuningPackage>
) {
    /** Solo lo que se le puede ofrecer a ese vehículo. */
    fun compatibleWith(vehicleCategory: String): PartsCatalog {
        val compatibleParts = parts.filter { it.isCompatibleWith(vehicleCategory) }
        val compatibleIds = compatibleParts.map { it.id }.toSet()
        return PartsCatalog(
            parts = compatibleParts,
            packages = packages
                .map { pkg -> pkg.copy(partIds = pkg.partIds.filter { it in compatibleIds }) }
                .filter { it.partIds.isNotEmpty() }
        )
    }
}

/**
 * CATALOG: piezas del catálogo. OWN_PART: el usuario trae la foto de una
 * pieza. FREE: texto libre, solo imagen, sin precio ni especificaciones.
 */
enum class TuningMode { CATALOG, OWN_PART, FREE }

/**
 * Especificaciones numéricas de un vehículo, para calcular. Cualquiera puede
 * faltar (el catálogo de vehículos guarda texto, no números).
 */
data class VehicleSpecs(
    val hp: Double? = null,
    val weightKg: Double? = null,
    val zeroTo100: Double? = null,
    val weightIsEstimated: Boolean = false
)

/** Resultado de [com.example.motormates.data.util.SpecCalculator]. */
data class TuningSummary(
    val before: VehicleSpecs = VehicleSpecs(),
    val after: VehicleSpecs = VehicleSpecs(),
    val hpDeltaPct: Double = 0.0,
    val torqueDeltaPct: Double = 0.0,
    val weightDeltaKg: Double = 0.0,
    val heightDeltaMm: Int = 0,
    val totalMinCop: Long = 0,
    val totalMaxCop: Long = 0,
    val partsCount: Int = 0,
    /** true si la suma de ganancias de potencia se recortó al tope. */
    val hpGainCapped: Boolean = false,
    val hasEstimatedPrices: Boolean = false
) {
    val hpDelta: Double?
        get() = if (before.hp != null && after.hp != null) after.hp - before.hp else null
}

/** Ficha técnica que redacta la AI a partir de los números ya calculados. */
data class TuningInsights(
    val summary: String = "",
    val handling: String = "",
    val consumption: String = "",
    val warranty: String = "",
    val legal: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)

data class GroundingSource(val title: String, val uri: String)

/**
 * Respuesta con búsqueda de Google. Las fuentes y las sugerencias de
 * búsqueda (searchEntryPointHtml) son obligatorias de mostrar según los
 * términos de uso del grounding.
 */
data class GroundedAnswer(
    val text: String,
    val sources: List<GroundingSource> = emptyList(),
    val searchEntryPointHtml: String? = null
) {
    val isGrounded: Boolean get() = sources.isNotEmpty() || searchEntryPointHtml != null
}

/** Imagen en memoria (JPEG/PNG). Se usa ByteArray para no atar el dominio a Android. */
class TuningImage(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is TuningImage && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()
}

/** Una versión modificada guardada ("Build"). */
data class Build(
    val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val ownerId: String,
    val ownerName: String?,
    val baseImageUrl: String?,
    val resultImageUrl: String,
    val partIds: List<String>,
    val partNames: List<String>,
    val mode: TuningMode,
    val prompt: String?,
    val summary: TuningSummary,
    val insights: TuningInsights?,
    val published: Boolean,
    val createdAt: Long
)

/** Lo que hace falta para guardar un Build (el id y las URLs los pone la capa de datos). */
data class BuildDraft(
    val vehicleId: String,
    val vehicleName: String,
    val baseImageUrl: String?,
    val parts: List<Part>,
    val mode: TuningMode,
    val prompt: String?,
    val summary: TuningSummary,
    val insights: TuningInsights?
)

internal fun String.normalizedKey(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
