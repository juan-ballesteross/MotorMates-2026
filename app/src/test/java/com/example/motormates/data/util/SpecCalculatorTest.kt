package com.example.motormates.data.util

import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.VehicleSpecs
import com.google.common.truth.Truth.assertThat
import kotlin.math.pow
import org.junit.Test

class SpecCalculatorTest {

    private val base = VehicleSpecs(hp = 200.0, weightKg = 1400.0, zeroTo100 = 8.0, weightIsEstimated = true)

    private fun part(
        id: String,
        category: PartCategory = PartCategory.ESCAPE,
        hp: Double = 0.0,
        torque: Double = 0.0,
        weight: Double = 0.0,
        height: Int = 0,
        min: Long = 1_000_000,
        max: Long = 2_000_000,
        priceSource: String = Part.PRICE_SOURCE_ESTIMATED
    ) = Part(
        id = id, name = id, brand = "", category = category,
        priceMinCop = min, priceMaxCop = max, priceSource = priceSource,
        hpDeltaPct = hp, torqueDeltaPct = torque, weightDeltaKg = weight, heightDeltaMm = height
    )

    @Test
    fun `sin piezas no cambia nada y el costo es cero`() {
        val summary = SpecCalculator.calculate(base, emptyList())

        assertThat(summary.after.hp).isEqualTo(200.0)
        assertThat(summary.after.weightKg).isEqualTo(1400.0)
        assertThat(summary.after.zeroTo100).isWithin(1e-9).of(8.0)
        assertThat(summary.totalMinCop).isEqualTo(0)
        assertThat(summary.totalMaxCop).isEqualTo(0)
        assertThat(summary.partsCount).isEqualTo(0)
        assertThat(summary.hpDelta).isEqualTo(0.0)
    }

    @Test
    fun `suma porcentajes de potencia, peso, altura y precios`() {
        val parts = listOf(
            part("escape", hp = 4.0, torque = 3.0, weight = -8.0, min = 5_000_000, max = 12_000_000),
            part("ecu", PartCategory.ECU, hp = 15.0, torque = 20.0, min = 2_500_000, max = 5_000_000),
            part("muelles", PartCategory.SUSPENSION, weight = -1.0, height = -30, min = 1_500_000, max = 3_000_000)
        )

        val summary = SpecCalculator.calculate(base, parts)

        assertThat(summary.hpDeltaPct).isEqualTo(19.0)
        assertThat(summary.after.hp).isWithin(1e-9).of(238.0)
        assertThat(summary.hpDelta).isWithin(1e-9).of(38.0)
        assertThat(summary.torqueDeltaPct).isEqualTo(23.0)
        assertThat(summary.weightDeltaKg).isEqualTo(-9.0)
        assertThat(summary.after.weightKg).isEqualTo(1391.0)
        assertThat(summary.heightDeltaMm).isEqualTo(-30)
        assertThat(summary.totalMinCop).isEqualTo(9_000_000)
        assertThat(summary.totalMaxCop).isEqualTo(20_000_000)
        assertThat(summary.partsCount).isEqualTo(3)
        assertThat(summary.hpGainCapped).isFalse()
    }

    @Test
    fun `la ganancia de potencia se limita al tope`() {
        val parts = listOf(
            part("stage2", PartCategory.ECU, hp = 25.0),
            part("escape", hp = 5.0),
            part("admision", PartCategory.ADMISION, hp = 10.0)
        )

        val summary = SpecCalculator.calculate(base, parts)

        assertThat(summary.hpDeltaPct).isEqualTo(SpecCalculator.MAX_TOTAL_GAIN_PCT)
        assertThat(summary.after.hp).isWithin(1e-9).of(200.0 * 1.35)
        assertThat(summary.hpGainCapped).isTrue()
    }

    @Test
    fun `0-100 baja cuando mejora la relacion peso potencia`() {
        val summary = SpecCalculator.calculate(base, listOf(part("ecu", PartCategory.ECU, hp = 15.0)))

        val expected = 8.0 * ((1400.0 / 230.0) / (1400.0 / 200.0)).pow(SpecCalculator.ZERO_TO_100_EXPONENT)
        assertThat(summary.after.zeroTo100).isWithin(1e-9).of(expected)
        assertThat(summary.after.zeroTo100!!).isLessThan(8.0)
    }

    @Test
    fun `0-100 sube si solo se agrega peso`() {
        val summary = SpecCalculator.calculate(base, listOf(part("lift", PartCategory.SUSPENSION, weight = 35.0)))

        assertThat(summary.after.zeroTo100!!).isGreaterThan(8.0)
    }

    @Test
    fun `sin potencia base no se inventa la potencia ni el 0-100`() {
        val sinHp = base.copy(hp = null)

        val summary = SpecCalculator.calculate(sinHp, listOf(part("ecu", PartCategory.ECU, hp = 15.0)))

        assertThat(summary.after.hp).isNull()
        assertThat(summary.after.zeroTo100).isNull()
        assertThat(summary.hpDelta).isNull()
        // El porcentaje sí se reporta: es dato del catálogo, no un cálculo.
        assertThat(summary.hpDeltaPct).isEqualTo(15.0)
    }

    @Test
    fun `marca si hay precios estimados`() {
        val referencia = part("a", priceSource = "referencia")
        val estimado = part("b", PartCategory.RINES)

        assertThat(SpecCalculator.calculate(base, listOf(referencia)).hasEstimatedPrices).isFalse()
        assertThat(SpecCalculator.calculate(base, listOf(referencia, estimado)).hasEstimatedPrices).isTrue()
    }

    @Test
    fun `el peso nunca queda en cero o negativo`() {
        val summary = SpecCalculator.calculate(base.copy(weightKg = 10.0), listOf(part("x", weight = -50.0)))

        assertThat(summary.after.weightKg).isEqualTo(1.0)
    }
}
