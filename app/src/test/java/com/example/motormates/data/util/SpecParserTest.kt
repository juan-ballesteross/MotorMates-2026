package com.example.motormates.data.util

import com.example.motormates.data.model.Vehicle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpecParserTest {

    @Test
    fun `parseNumber saca el numero de textos con unidad`() {
        assertThat(SpecParser.parseNumber("502 hp")).isEqualTo(502.0)
        assertThat(SpecParser.parseNumber("3.4 s")).isEqualTo(3.4)
        assertThat(SpecParser.parseNumber("318 km/h")).isEqualTo(318.0)
    }

    @Test
    fun `parseNumber acepta coma decimal`() {
        assertThat(SpecParser.parseNumber("27,0 s")).isEqualTo(27.0)
    }

    @Test
    fun `parseNumber devuelve null si no hay numero o el texto es vacio`() {
        assertThat(SpecParser.parseNumber(null)).isNull()
        assertThat(SpecParser.parseNumber("")).isNull()
        assertThat(SpecParser.parseNumber("RWD")).isNull()
    }

    @Test
    fun `estimatedWeightKg ignora tildes y mayusculas`() {
        assertThat(SpecParser.estimatedWeightKg("Clásico")).isEqualTo(1250.0)
        assertThat(SpecParser.estimatedWeightKg("clasico")).isEqualTo(1250.0)
        assertThat(SpecParser.estimatedWeightKg("ELÉCTRICO")).isEqualTo(1850.0)
        assertThat(SpecParser.estimatedWeightKg("Desconocida")).isEqualTo(1600.0)
    }

    @Test
    fun `specsOf arma las especificaciones con peso estimado`() {
        val porsche = Vehicle(
            id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
            imageUrl = null, potencia = "502 hp", aceleracion = "3.4 s", velocidadMaxima = "318 km/h", traccion = "RWD"
        )

        val specs = SpecParser.specsOf(porsche)

        assertThat(specs.hp).isEqualTo(502.0)
        assertThat(specs.zeroTo100).isEqualTo(3.4)
        assertThat(specs.weightKg).isEqualTo(1450.0)
        assertThat(specs.weightIsEstimated).isTrue()
    }

    @Test
    fun `specsOf deja en null la potencia si no viene`() {
        val sinDatos = Vehicle(
            id = "9", brand = "X", model = "Y", year = 2020, category = "SUV",
            imageUrl = null, potencia = null, aceleracion = "—", velocidadMaxima = null, traccion = null
        )

        val specs = SpecParser.specsOf(sinDatos)

        assertThat(specs.hp).isNull()
        assertThat(specs.zeroTo100).isNull()
    }
}
