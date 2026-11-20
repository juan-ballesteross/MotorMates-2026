package com.example.motormates.data.util

import com.example.motormates.data.model.Part
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.model.Vehicle
import java.util.Locale

/**
 * Arma los prompts que se mandan a Gemini. El usuario nunca escribe un
 * prompt en el modo catálogo: elige piezas y aquí se traduce su elección.
 *
 * Los prompts de imagen van en inglés (los modelos de imagen siguen mejor
 * instrucciones de edición en inglés); los de texto piden la respuesta en
 * español de Colombia.
 */
object TuningPromptBuilder {

    /** Reglas comunes a toda edición: que el carro siga siendo el mismo. */
    private const val PRESERVATION_RULES =
        "Keep the exact same car, body shape, camera angle, framing, background, " +
            "lighting and reflections. Do not change anything that is not listed. " +
            "Do not add text, logos, watermarks or people. The result must look like a real photograph."

    fun vehicleName(vehicle: Vehicle): String = "${vehicle.brand} ${vehicle.model}".trim()

    /**
     * @param partsWithReference ids de las piezas a las que se les adjunta
     *   imagen de referencia, en el mismo orden en que van las imágenes
     *   después de la foto del carro (imagen 1).
     */
    fun catalogImagePrompt(
        vehicle: Vehicle,
        parts: List<Part>,
        partsWithReference: List<String> = emptyList()
    ): String {
        val visibleParts = parts.filter { it.visible && it.visualPrompt.isNotBlank() }
        val lines = visibleParts.mapIndexed { index, part ->
            val referenceIndex = partsWithReference.indexOf(part.id)
            val reference = if (referenceIndex >= 0) {
                " Use image ${referenceIndex + 2} as the exact reference for this part's design."
            } else ""
            "${index + 1}. ${part.category.label}: ${part.visualPrompt}.$reference"
        }
        return buildString {
            append("Image 1 is a photo of a ${vehicle.year} ${vehicleName(vehicle)}. ")
            append("Edit image 1 to install exactly these modifications on the car:\n")
            lines.forEach { append(it).append('\n') }
            append(PRESERVATION_RULES)
        }
    }

    fun ownPartImagePrompt(vehicle: Vehicle, description: String): String = buildString {
        append("Image 1 is a photo of a ${vehicle.year} ${vehicleName(vehicle)}. ")
        append("Image 2 shows a real aftermarket car part")
        if (description.isNotBlank()) append(" described by the owner as: \"${description.trim()}\"")
        append(". Install exactly that part on the car in image 1, matching its design, ")
        append("color and finish, scaled and positioned realistically where it belongs. ")
        append(PRESERVATION_RULES)
    }

    fun freeImagePrompt(vehicle: Vehicle, idea: String): String = buildString {
        append("Image 1 is a photo of a ${vehicle.year} ${vehicleName(vehicle)}. ")
        append("Edit it following this idea from the owner: \"${idea.trim()}\". ")
        append(PRESERVATION_RULES)
    }

    fun refinePrompt(instruction: String): String =
        "Apply this change to the last image: \"${instruction.trim()}\". $PRESERVATION_RULES"

    /**
     * La AI recibe los números ya calculados y solo los explica. Se le pide
     * explícitamente no inventar cifras nuevas.
     */
    fun insightsPrompt(vehicle: Vehicle, parts: List<Part>, summary: TuningSummary): String {
        val partLines = parts.joinToString("\n") { part ->
            "- ${part.category.label}: ${part.name} (${part.brand}). " +
                "Rango ${CopFormat.range(part.priceMinCop, part.priceMaxCop)}. " +
                "Potencia ${signed(part.hpDeltaPct)} %, peso ${signed(part.weightDeltaKg)} kg, " +
                "altura ${signed(part.heightDeltaMm.toDouble())} mm."
        }
        val before = summary.before
        val after = summary.after
        // Se arma por líneas (y no con trimIndent) porque partLines trae
        // varias líneas sin sangría y rompería el recorte.
        return listOf(
            "Eres un ingeniero automotriz que asesora a entusiastas en Colombia.",
            "Vehículo: ${vehicle.year} ${vehicleName(vehicle)} (${vehicle.category}), " +
                "tracción ${vehicle.traccion ?: "desconocida"}.",
            "Modificaciones elegidas:",
            partLines,
            "",
            "Cálculos ya hechos por la app (NO los cambies ni inventes otros números):",
            "- Potencia: ${number(before.hp)} hp -> ${number(after.hp)} hp (${signed(summary.hpDeltaPct)} %).",
            "- Torque: ${signed(summary.torqueDeltaPct)} %.",
            "- Peso: ${number(before.weightKg)} kg -> ${number(after.weightKg)} kg (estimado).",
            "- 0-100 km/h: ${decimal(before.zeroTo100)} s -> ${decimal(after.zeroTo100)} s (estimado).",
            "- Altura: ${signed(summary.heightDeltaMm.toDouble())} mm.",
            "- Costo total: ${CopFormat.range(summary.totalMinCop, summary.totalMaxCop)}.",
            "",
            "Explica en español de Colombia, claro y breve (máximo 2 frases por campo):",
            "- summary: resumen del resultado del build.",
            "- handling: cómo cambia el manejo y la comodidad.",
            "- consumption: efecto en consumo de combustible o batería.",
            "- warranty: riesgos para la garantía y el desgaste.",
            "- legal: puntos legales en Colombia a revisar (por ejemplo cambio de color ante el " +
                "organismo de tránsito, polarizado, ruido del escape, altura). Si no aplica ninguno, lista vacía.",
            "- tips: recomendaciones prácticas de instalación o mantenimiento."
        ).joinToString("\n")
    }

    fun livePricePrompt(part: Part): String = """
        Busca el precio actual en Colombia (en pesos colombianos, COP) de esta pieza para carro:
        "${part.name}" (${part.brand}).
        Responde en español en máximo 3 frases: el rango de precio que encontraste, en qué tiendas
        o sitios de Colombia, y una advertencia si los precios varían mucho. No inventes precios:
        si no encuentras datos de Colombia, dilo.
    """.trimIndent()

    fun identifyPartPrompt(description: String): String = buildString {
        append("La imagen muestra una pieza de carro (accesorio o modificación). ")
        if (description.isNotBlank()) append("El usuario la describe así: \"${description.trim()}\". ")
        append("Identifica qué pieza es (tipo y, si se puede, marca o estilo) y busca su rango de ")
        append("precio actual en Colombia en pesos colombianos. Responde en español en máximo 3 frases. ")
        append("No inventes precios: si no encuentras datos de Colombia, dilo.")
    }

    private fun number(value: Double?): String =
        value?.let { String.format(Locale.ROOT, "%.0f", it) } ?: "desconocido"

    private fun decimal(value: Double?): String =
        value?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "desconocido"

    private fun signed(value: Double): String {
        val formatted = if (value % 1.0 == 0.0) String.format(Locale.ROOT, "%.0f", value)
        else String.format(Locale.ROOT, "%.1f", value)
        return if (value > 0) "+$formatted" else formatted
    }
}
