package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Validación del formulario para publicar un vehículo. */
class VehicleDraftTest {

    private val currentYear = 2026

    private val valid = VehicleFormInput(
        brand = "  Mazda ",
        model = "MX-5",
        year = "2020",
        category = "Deportivo",
        potenciaHp = "181",
        aceleracionS = "6,5",
        velocidadKmh = "219",
        traccion = "RWD"
    )

    @Test
    fun `un formulario completo genera el borrador con las unidades del catalogo`() {
        val result = valid.validate(currentYear)

        assertThat(result.isValid).isTrue()
        val draft = result.draft!!
        assertThat(draft.brand).isEqualTo("Mazda")
        assertThat(draft.year).isEqualTo(2020)
        assertThat(draft.potencia).isEqualTo("181 hp")
        assertThat(draft.aceleracion).isEqualTo("6.5 s")
        assertThat(draft.velocidadMaxima).isEqualTo("219 km/h")
        assertThat(draft.traccion).isEqualTo("RWD")
    }

    @Test
    fun `las especificaciones salen en un formato que el comparador entiende`() {
        val draft = valid.validate(currentYear).draft!!

        assertThat(parseSpecNumber(draft.potencia)).isEqualTo(181.0)
        assertThat(parseSpecNumber(draft.aceleracion)).isEqualTo(6.5)
        assertThat(parseSpecNumber(draft.velocidadMaxima)).isEqualTo(219.0)
    }

    @Test
    fun `las especificaciones son opcionales`() {
        val result = valid.copy(potenciaHp = "", aceleracionS = " ", velocidadKmh = "", traccion = null)
            .validate(currentYear)

        assertThat(result.isValid).isTrue()
        assertThat(result.draft!!.potencia).isNull()
        assertThat(result.draft!!.aceleracion).isNull()
        assertThat(result.draft!!.traccion).isNull()
    }

    @Test
    fun `marca modelo ano y categoria son obligatorios`() {
        val result = VehicleFormInput().validate(currentYear)

        assertThat(result.isValid).isFalse()
        assertThat(result.errors.keys).containsExactly(
            VehicleFormField.BRAND,
            VehicleFormField.MODEL,
            VehicleFormField.YEAR,
            VehicleFormField.CATEGORY
        )
    }

    @Test
    fun `el ano no puede ser del futuro lejano ni anterior al primer carro`() {
        assertThat(valid.copy(year = "2028").validate(currentYear).errors).containsKey(VehicleFormField.YEAR)
        assertThat(valid.copy(year = "1800").validate(currentYear).errors).containsKey(VehicleFormField.YEAR)
        // El modelo del año siguiente sí se permite.
        assertThat(valid.copy(year = "2027").validate(currentYear).isValid).isTrue()
    }

    @Test
    fun `una categoria que no es del catalogo no vale`() {
        val result = valid.copy(category = "Nave espacial").validate(currentYear)

        assertThat(result.errors).containsKey(VehicleFormField.CATEGORY)
    }

    @Test
    fun `especificaciones fuera de rango marcan su campo`() {
        val result = valid.copy(potenciaHp = "5000", aceleracionS = "0.2", velocidadKmh = "abc")
            .validate(currentYear)

        assertThat(result.errors.keys).containsExactly(
            VehicleFormField.POTENCIA,
            VehicleFormField.ACELERACION,
            VehicleFormField.VELOCIDAD
        )
    }

    @Test
    fun `nombres demasiado largos no valen`() {
        val result = valid.copy(model = "x".repeat(MAX_NAME_LENGTH + 1)).validate(currentYear)

        assertThat(result.errors).containsKey(VehicleFormField.MODEL)
    }

    // ---------- Prellenar para editar ----------

    @Test
    fun `un vehiculo publicado se convierte en el formulario y vuelve igual`() {
        val draft = valid.validate(currentYear).draft!!
        val vehicle = Vehicle(
            id = "v1", brand = draft.brand, model = draft.model, year = draft.year, category = draft.category,
            imageUrl = null, potencia = draft.potencia, aceleracion = draft.aceleracion,
            velocidadMaxima = draft.velocidadMaxima, traccion = draft.traccion
        )

        val form = vehicle.toFormInput()

        assertThat(form.potenciaHp).isEqualTo("181")
        assertThat(form.aceleracionS).isEqualTo("6.5")
        assertThat(form.velocidadKmh).isEqualTo("219")
        // Ida y vuelta: guardar sin tocar nada deja el mismo vehículo.
        assertThat(form.validate(currentYear).draft).isEqualTo(draft)
    }

    @Test
    fun `datos del catalogo que el formulario no conoce quedan vacios`() {
        val vehicle = Vehicle(
            id = "v1", brand = "Jeep", model = "Wrangler", year = 2023, category = "Todoterreno",
            imageUrl = null, potencia = null, aceleracion = "—", velocidadMaxima = null, traccion = "4WD"
        )

        val form = vehicle.toFormInput()

        assertThat(form.category).isNull()
        assertThat(form.aceleracionS).isEmpty()
        assertThat(form.traccion).isNull()
    }
}
