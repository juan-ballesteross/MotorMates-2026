package com.example.motormates.data.util

import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.VehicleSpecs
import com.example.motormates.data.model.normalizedKey

/**
 * Los vehículos guardan sus especificaciones como texto con unidad
 * ("502 hp", "3.4 s"). No se migra el modelo: este parser saca el número
 * para poder calcular. Si el texto no trae número, devuelve null y la
 * pantalla muestra "—" en vez de inventar.
 */
object SpecParser {

    private val NUMBER = Regex("""(\d+(?:[.,]\d+)?)""")

    /** Primer número del texto. Acepta coma o punto decimal: "27,0 s" → 27.0. */
    fun parseNumber(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val match = NUMBER.find(text) ?: return null
        return match.value.replace(',', '.').toDoubleOrNull()
    }

    /**
     * El catálogo de vehículos no guarda el peso, así que se usa un valor
     * típico por categoría y se marca como estimado.
     */
    fun estimatedWeightKg(category: String): Double = when (category.normalizedKey()) {
        "deportivo" -> 1450.0
        "suv" -> 2200.0
        "clasico" -> 1250.0
        "electrico" -> 1850.0
        "pickup" -> 2100.0
        else -> 1600.0
    }

    fun specsOf(vehicle: Vehicle): VehicleSpecs = VehicleSpecs(
        hp = parseNumber(vehicle.potencia)?.takeIf { it > 0 },
        weightKg = estimatedWeightKg(vehicle.category),
        zeroTo100 = parseNumber(vehicle.aceleracion)?.takeIf { it > 0 },
        weightIsEstimated = true
    )
}
