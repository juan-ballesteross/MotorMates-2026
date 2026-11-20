package com.example.motormates.data.util

import com.example.motormates.data.model.Part
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.VehicleSpecs
import kotlin.math.pow

/**
 * Calcula precio y rendimiento de una combinación de piezas. Es Kotlin puro
 * (sin Android ni AI) para que se pueda probar y para que los números de la
 * ficha nunca dependan de lo que "opine" el modelo.
 *
 * Reglas:
 * - Potencia y torque: se suman los porcentajes declarados de cada pieza,
 *   con un tope de [MAX_TOTAL_GAIN_PCT]. Las piezas de calle no se acumulan
 *   sin límite en la vida real (un filtro + escape + Stage 2 no da +40 %).
 * - Peso: peso base + suma de los deltas de cada pieza.
 * - 0-100 km/h: escala con la relación peso/potencia elevada a
 *   [ZERO_TO_100_EXPONENT]. Es una aproximación (la tracción limita la
 *   salida), por eso la pantalla lo marca como estimado.
 * - Precio: suma de mínimos y suma de máximos del rango de cada pieza.
 */
object SpecCalculator {

    const val MAX_TOTAL_GAIN_PCT = 35.0
    const val ZERO_TO_100_EXPONENT = 0.8

    fun calculate(before: VehicleSpecs, parts: List<Part>): TuningSummary {
        val rawHpGain = parts.sumOf { it.hpDeltaPct }
        val hpGain = rawHpGain.coerceIn(-MAX_TOTAL_GAIN_PCT, MAX_TOTAL_GAIN_PCT)
        val torqueGain = parts.sumOf { it.torqueDeltaPct }
            .coerceIn(-MAX_TOTAL_GAIN_PCT, MAX_TOTAL_GAIN_PCT)
        val weightDelta = parts.sumOf { it.weightDeltaKg }

        val hpAfter = before.hp?.let { it * (1 + hpGain / 100.0) }
        val weightAfter = before.weightKg?.let { (it + weightDelta).coerceAtLeast(1.0) }
        val zeroTo100After = estimateZeroTo100(
            before.zeroTo100, before.hp, before.weightKg, hpAfter, weightAfter
        )

        return TuningSummary(
            before = before,
            after = before.copy(hp = hpAfter, weightKg = weightAfter, zeroTo100 = zeroTo100After),
            hpDeltaPct = hpGain,
            torqueDeltaPct = torqueGain,
            weightDeltaKg = weightDelta,
            heightDeltaMm = parts.sumOf { it.heightDeltaMm },
            totalMinCop = parts.sumOf { it.priceMinCop },
            totalMaxCop = parts.sumOf { it.priceMaxCop },
            partsCount = parts.size,
            hpGainCapped = rawHpGain > MAX_TOTAL_GAIN_PCT,
            hasEstimatedPrices = parts.any { it.isPriceEstimated }
        )
    }

    /** t_nuevo ≈ t_actual × ((peso/HP)_nuevo ÷ (peso/HP)_actual)^0.8 */
    internal fun estimateZeroTo100(
        time: Double?,
        hpBefore: Double?,
        weightBefore: Double?,
        hpAfter: Double?,
        weightAfter: Double?
    ): Double? {
        if (time == null || hpBefore == null || weightBefore == null ||
            hpAfter == null || weightAfter == null || hpBefore <= 0 || hpAfter <= 0
        ) return null
        val ratioBefore = weightBefore / hpBefore
        val ratioAfter = weightAfter / hpAfter
        return time * (ratioAfter / ratioBefore).pow(ZERO_TO_100_EXPONENT)
    }
}
