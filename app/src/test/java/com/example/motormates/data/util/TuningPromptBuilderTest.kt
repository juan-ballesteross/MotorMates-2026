package com.example.motormates.data.util

import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.VehicleSpecs
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TuningPromptBuilderTest {

    private val porsche = Vehicle(
        id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = "https://example.com/911.jpg", potencia = "502 hp", aceleracion = "3.4 s",
        velocidadMaxima = "318 km/h", traccion = "RWD"
    )

    private val rines = Part(
        id = "rin", name = "Rin 18", brand = "OZ", category = PartCategory.RINES,
        priceMinCop = 4_500_000, priceMaxCop = 7_500_000,
        visualPrompt = "18-inch matte black wheels"
    )
    private val ala = Part(
        id = "ala", name = "Ala GT", brand = "Carbono", category = PartCategory.AERO,
        priceMinCop = 2_500_000, priceMaxCop = 6_000_000,
        visualPrompt = "a large carbon fiber GT wing"
    )
    private val ecu = Part(
        id = "ecu", name = "Stage 1", brand = "APR", category = PartCategory.ECU,
        priceMinCop = 2_500_000, priceMaxCop = 5_000_000, hpDeltaPct = 15.0,
        visible = false, visualPrompt = ""
    )

    @Test
    fun `el prompt de catalogo describe las piezas visibles y omite las ocultas`() {
        val prompt = TuningPromptBuilder.catalogImagePrompt(porsche, listOf(rines, ala, ecu))

        assertThat(prompt).contains("2024 Porsche 911 GT3")
        assertThat(prompt).contains("1. Rines: 18-inch matte black wheels.")
        assertThat(prompt).contains("2. Aero: a large carbon fiber GT wing.")
        assertThat(prompt).doesNotContain("Stage 1")
        assertThat(prompt).contains("Keep the exact same car")
    }

    @Test
    fun `las piezas con foto apuntan a su imagen de referencia en orden`() {
        val prompt = TuningPromptBuilder.catalogImagePrompt(
            porsche, listOf(rines, ala), partsWithReference = listOf("ala", "rin")
        )

        // La imagen 1 es el carro; las referencias empiezan en la 2.
        assertThat(prompt).contains("18-inch matte black wheels. Use image 3 as the exact reference")
        assertThat(prompt).contains("GT wing. Use image 2 as the exact reference")
    }

    @Test
    fun `el prompt de pieza propia incluye la descripcion del usuario`() {
        val prompt = TuningPromptBuilder.ownPartImagePrompt(porsche, "  rin bronce de 19  ")

        assertThat(prompt).contains("Image 2 shows a real aftermarket car part")
        assertThat(prompt).contains("\"rin bronce de 19\"")
    }

    @Test
    fun `el prompt de pieza propia funciona sin descripcion`() {
        val prompt = TuningPromptBuilder.ownPartImagePrompt(porsche, "")

        assertThat(prompt).doesNotContain("described by the owner")
        assertThat(prompt).contains("Install exactly that part")
    }

    @Test
    fun `el prompt de insights trae los numeros calculados y pide no inventar`() {
        val summary = SpecCalculator.calculate(
            VehicleSpecs(hp = 502.0, weightKg = 1450.0, zeroTo100 = 3.4, weightIsEstimated = true),
            listOf(rines, ecu)
        )

        val prompt = TuningPromptBuilder.insightsPrompt(porsche, listOf(rines, ecu), summary)

        assertThat(prompt).contains("502 hp -> 577 hp (+15 %)")
        assertThat(prompt).contains("NO los cambies ni inventes otros números")
        assertThat(prompt).contains("Costo total: $7 M – $12,5 M")
        assertThat(prompt).contains("- Motor: Stage 1 (APR)")
        // Sin líneas con sangría sobrante (el prompt se arma por líneas).
        assertThat(prompt.lines().none { it.startsWith("    ") }).isTrue()
    }

    @Test
    fun `el prompt de precio pide pesos colombianos y no inventar`() {
        val prompt = TuningPromptBuilder.livePricePrompt(rines)

        assertThat(prompt).contains("\"Rin 18\" (OZ)")
        assertThat(prompt).contains("COP")
        assertThat(prompt).contains("No inventes precios")
    }
}
