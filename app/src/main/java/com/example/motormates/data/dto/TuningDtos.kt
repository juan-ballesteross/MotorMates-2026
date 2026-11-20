package com.example.motormates.data.dto

import com.example.motormates.data.model.Build
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.PartsCatalog
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningPackage
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.VehicleSpecs
import com.google.firebase.firestore.DocumentId

/**
 * DTOs de AI Tuning. Los mismos sirven para Firestore (necesita constructor
 * sin argumentos: todo con valor por defecto) y para Gson (catálogo local en
 * assets/parts_catalog.json). Gson puede dejar en null campos no nulables si
 * faltan en el JSON, por eso toDomain() revisa todo con cuidado.
 */
data class PartDto(
    @DocumentId val id: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val imageIsReference: Boolean? = null,
    val sourceUrl: String? = null,
    val priceMinCop: Long? = null,
    val priceMaxCop: Long? = null,
    val priceSource: String? = null,
    val priceCheckedAt: String? = null,
    val hpDeltaPct: Double? = null,
    val torqueDeltaPct: Double? = null,
    val weightDeltaKg: Double? = null,
    val heightDeltaMm: Int? = null,
    val compatibleCategories: List<String>? = null,
    val visible: Boolean? = null,
    val visualPrompt: String? = null,
    val perfSource: String? = null,
    val colorHex: String? = null,
    val finish: String? = null
) {
    /** null si la pieza está incompleta (sin id, nombre o categoría válida). */
    fun toDomain(): Part? {
        val partId = id?.takeIf { it.isNotBlank() } ?: return null
        val partName = name?.takeIf { it.isNotBlank() } ?: return null
        val partCategory = PartCategory.fromKey(category) ?: return null
        val min = priceMinCop ?: 0L
        val max = (priceMaxCop ?: min).coerceAtLeast(min)
        return Part(
            id = partId,
            name = partName,
            brand = brand.orEmpty(),
            category = partCategory,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() },
            imageIsReference = imageIsReference ?: true,
            sourceUrl = sourceUrl?.takeIf { it.isNotBlank() },
            priceMinCop = min,
            priceMaxCop = max,
            priceSource = priceSource ?: Part.PRICE_SOURCE_ESTIMATED,
            priceCheckedAt = priceCheckedAt,
            hpDeltaPct = hpDeltaPct ?: 0.0,
            torqueDeltaPct = torqueDeltaPct ?: 0.0,
            weightDeltaKg = weightDeltaKg ?: 0.0,
            heightDeltaMm = heightDeltaMm ?: 0,
            compatibleCategories = compatibleCategories.orEmpty(),
            visible = visible ?: true,
            visualPrompt = visualPrompt.orEmpty(),
            perfSource = perfSource,
            colorHex = colorHex,
            finish = finish
        )
    }
}

data class TuningPackageDto(
    @DocumentId val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val partIds: List<String>? = null
) {
    fun toDomain(): TuningPackage? {
        val packageId = id?.takeIf { it.isNotBlank() } ?: return null
        return TuningPackage(
            id = packageId,
            name = name ?: packageId,
            description = description.orEmpty(),
            partIds = partIds.orEmpty()
        )
    }
}

/** Forma del archivo assets/parts_catalog.json. */
data class PartsCatalogJson(
    val version: Int? = null,
    val parts: List<PartDto>? = null,
    val packages: List<TuningPackageDto>? = null
) {
    fun toDomain(): PartsCatalog = PartsCatalog(
        parts = parts.orEmpty().mapNotNull { it.toDomain() },
        packages = packages.orEmpty().mapNotNull { it.toDomain() }
    )
}

data class SpecsDto(
    val hp: Double? = null,
    val weightKg: Double? = null,
    val zeroTo100: Double? = null,
    val weightIsEstimated: Boolean = false
) {
    fun toDomain() = VehicleSpecs(hp, weightKg, zeroTo100, weightIsEstimated)

    companion object {
        fun from(specs: VehicleSpecs) =
            SpecsDto(specs.hp, specs.weightKg, specs.zeroTo100, specs.weightIsEstimated)
    }
}

/**
 * Lo usa Gson para leer la ficha JSON que devuelve Gemini y Firestore para
 * guardarla dentro del Build.
 */
data class InsightsDto(
    val summary: String? = null,
    val handling: String? = null,
    val consumption: String? = null,
    val warranty: String? = null,
    val legal: List<String>? = null,
    val tips: List<String>? = null
) {
    fun toDomain() = TuningInsights(
        summary = summary.orEmpty(),
        handling = handling.orEmpty(),
        consumption = consumption.orEmpty(),
        warranty = warranty.orEmpty(),
        legal = legal.orEmpty().filter { it.isNotBlank() },
        tips = tips.orEmpty().filter { it.isNotBlank() }
    )

    companion object {
        fun from(insights: TuningInsights) = InsightsDto(
            insights.summary, insights.handling, insights.consumption,
            insights.warranty, insights.legal, insights.tips
        )
    }
}

/** Documento de la colección "builds". */
data class BuildFirestoreDto(
    @DocumentId val id: String = "",
    val vehicleId: String = "",
    val vehicleName: String = "",
    val ownerId: String = "",
    val ownerName: String? = null,
    val baseImageUrl: String? = null,
    val resultImageUrl: String = "",
    val partIds: List<String> = emptyList(),
    val partNames: List<String> = emptyList(),
    val mode: String = TuningMode.CATALOG.name,
    val prompt: String? = null,
    val specsBefore: SpecsDto = SpecsDto(),
    val specsAfter: SpecsDto = SpecsDto(),
    val hpDeltaPct: Double = 0.0,
    val torqueDeltaPct: Double = 0.0,
    val weightDeltaKg: Double = 0.0,
    val heightDeltaMm: Int = 0,
    val totalMinCop: Long = 0,
    val totalMaxCop: Long = 0,
    val hasEstimatedPrices: Boolean = false,
    val insights: InsightsDto? = null,
    val published: Boolean = false,
    val createdAt: Long = 0
) {
    fun toDomain(): Build = Build(
        id = id,
        vehicleId = vehicleId,
        vehicleName = vehicleName,
        ownerId = ownerId,
        ownerName = ownerName,
        baseImageUrl = baseImageUrl,
        resultImageUrl = resultImageUrl,
        partIds = partIds,
        partNames = partNames,
        mode = TuningMode.entries.firstOrNull { it.name == mode } ?: TuningMode.CATALOG,
        prompt = prompt,
        summary = TuningSummary(
            before = specsBefore.toDomain(),
            after = specsAfter.toDomain(),
            hpDeltaPct = hpDeltaPct,
            torqueDeltaPct = torqueDeltaPct,
            weightDeltaKg = weightDeltaKg,
            heightDeltaMm = heightDeltaMm,
            totalMinCop = totalMinCop,
            totalMaxCop = totalMaxCop,
            partsCount = partIds.size,
            hasEstimatedPrices = hasEstimatedPrices
        ),
        insights = insights?.toDomain(),
        published = published,
        createdAt = createdAt
    )
}
