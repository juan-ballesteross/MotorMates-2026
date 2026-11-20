package com.example.motormates.data.source

import com.example.motormates.data.model.PartCategory
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Valida el catálogo REAL que trae la app (assets/parts_catalog.json), que
 * también sube el seeder. Si alguien edita el JSON y rompe algo (un id
 * repetido, un paquete que apunta a una pieza que no existe), falla aquí y
 * no en la demo.
 */
class PartsCatalogFileTest {

    // Las pruebas unitarias corren con el módulo app/ como directorio de trabajo.
    private val json = File("src/main/assets/parts_catalog.json").readText()
    private val catalog = PartsCatalogParser.parse(json)

    private val vehicleCategories = listOf("Deportivo", "SUV", "Clásico", "Eléctrico", "Pickup")

    @Test
    fun `todas las piezas del archivo son validas`() {
        val rawCount = Regex("\"id\":\\s*\"").findAll(json.substringBefore("\"packages\"")).count()

        assertThat(catalog.parts).isNotEmpty()
        // Ninguna se descartó en toDomain() por estar incompleta.
        assertThat(catalog.parts).hasSize(rawCount)
    }

    @Test
    fun `los ids son unicos`() {
        val ids = catalog.parts.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `los paquetes solo usan piezas que existen`() {
        val ids = catalog.parts.map { it.id }.toSet()
        catalog.packages.forEach { pkg ->
            assertThat(ids).containsAtLeastElementsIn(pkg.partIds)
        }
    }

    @Test
    fun `los precios son rangos validos`() {
        catalog.parts.forEach { part ->
            assertThat(part.priceMinCop).isGreaterThan(0)
            assertThat(part.priceMaxCop).isAtLeast(part.priceMinCop)
        }
    }

    @Test
    fun `las piezas visibles tienen descripcion visual y las ocultas no`() {
        catalog.parts.forEach { part ->
            if (part.visible) assertThat(part.visualPrompt).isNotEmpty() else assertThat(part.visualPrompt).isEmpty()
        }
    }

    @Test
    fun `los colores traen su hex y acabado`() {
        val colors = catalog.parts.filter { it.category == PartCategory.COLOR }
        assertThat(colors).isNotEmpty()
        colors.forEach { part ->
            assertThat(part.colorHex).matches("#[0-9A-Fa-f]{6}")
            assertThat(part.finish).isNotNull()
        }
    }

    @Test
    fun `las categorias compatibles existen en el catalogo de vehiculos`() {
        catalog.parts.flatMap { it.compatibleCategories }.forEach { category ->
            assertThat(vehicleCategories).contains(category)
        }
    }

    @Test
    fun `cada categoria de vehiculo tiene al menos un paquete y varias piezas`() {
        vehicleCategories.forEach { category ->
            val forVehicle = catalog.compatibleWith(category)
            assertThat(forVehicle.parts.size).isAtLeast(10)
            assertThat(forVehicle.packages).isNotEmpty()
        }
    }

    @Test
    fun `un electrico no recibe escape, admision ni reprogramacion`() {
        val electrico = catalog.compatibleWith("Eléctrico")

        assertThat(electrico.parts.map { it.category })
            .containsNoneOf(PartCategory.ESCAPE, PartCategory.ADMISION, PartCategory.ECU)
    }

    @Test
    fun `las fotos de piezas son https y las de colores no llevan foto`() {
        val withPhoto = catalog.parts.filter { it.imageUrl != null }

        assertThat(withPhoto).isNotEmpty()
        withPhoto.forEach { part -> assertThat(part.imageUrl).startsWith("https://") }
        // El color se pinta como muestra; una foto de otro carro confundiría a la AI.
        assertThat(catalog.parts.filter { it.category == PartCategory.COLOR }.all { it.imageUrl == null }).isTrue()
    }

    @Test
    fun `todas las piezas que no son color tienen foto en su tarjeta`() {
        val withoutPhoto = catalog.parts.filter { it.category != PartCategory.COLOR && it.imageUrl == null }

        assertThat(withoutPhoto.map { it.id }).isEmpty()
    }
}
