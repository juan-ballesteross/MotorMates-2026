package com.example.motormates.data.model

import java.util.Locale

/**
 * Publicar un vehículo propio: lo que escribe el usuario en el formulario,
 * su validación y el vehículo ya listo para guardar.
 *
 * Kotlin puro (sin Android) para probar la validación en la JVM.
 */

/** Mismas categorías que usa el catálogo, así los filtros y el comparador las entienden. */
val VEHICLE_CATEGORIES = listOf("Deportivo", "SUV", "Clásico", "Eléctrico", "Pickup")

val VEHICLE_TRACTIONS = listOf("RWD", "FWD", "AWD", "4x4")

enum class VehicleFormField { BRAND, MODEL, YEAR, CATEGORY, POTENCIA, ACELERACION, VELOCIDAD }

/** Lo que hay en el formulario, tal cual se escribió (todo texto). */
data class VehicleFormInput(
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val category: String? = null,
    val potenciaHp: String = "",
    val aceleracionS: String = "",
    val velocidadKmh: String = "",
    val traccion: String? = null
)

/**
 * Un vehículo validado y listo para publicar. Las especificaciones ya van
 * con su unidad ("502 hp", "3.4 s", "318 km/h"), igual que el catálogo:
 * así el comparador las lee sin casos especiales.
 */
data class VehicleDraft(
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val potencia: String?,
    val aceleracion: String?,
    val velocidadMaxima: String?,
    val traccion: String?
)

/** Lo que recibe la fuente de datos: el borrador más el dueño y la foto ya subida. */
data class NewVehicle(
    val draft: VehicleDraft,
    val ownerId: String,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val imageUrl: String?
)

data class VehicleFormValidation(
    val draft: VehicleDraft?,
    val errors: Map<VehicleFormField, String>
) {
    val isValid: Boolean get() = draft != null
}

const val MAX_NAME_LENGTH = 40
private const val FIRST_CAR_YEAR = 1886

/**
 * Valida el formulario. Marca, modelo, año y categoría son obligatorios;
 * las especificaciones son opcionales, pero si se escriben tienen que ser
 * números razonables. currentYear se recibe para poder probarlo.
 */
fun VehicleFormInput.validate(currentYear: Int): VehicleFormValidation {
    val errors = mutableMapOf<VehicleFormField, String>()

    val cleanBrand = brand.trim()
    val cleanModel = model.trim()
    when {
        cleanBrand.isEmpty() -> errors[VehicleFormField.BRAND] = "Escribe la marca"
        cleanBrand.length > MAX_NAME_LENGTH -> errors[VehicleFormField.BRAND] = "Máximo $MAX_NAME_LENGTH caracteres"
    }
    when {
        cleanModel.isEmpty() -> errors[VehicleFormField.MODEL] = "Escribe el modelo"
        cleanModel.length > MAX_NAME_LENGTH -> errors[VehicleFormField.MODEL] = "Máximo $MAX_NAME_LENGTH caracteres"
    }

    val parsedYear = year.trim().toIntOrNull()
    if (parsedYear == null || parsedYear !in FIRST_CAR_YEAR..(currentYear + 1)) {
        errors[VehicleFormField.YEAR] = "Año entre $FIRST_CAR_YEAR y ${currentYear + 1}"
    }

    if (category == null || category !in VEHICLE_CATEGORIES) {
        errors[VehicleFormField.CATEGORY] = "Elige una categoría"
    }

    val hp = optionalNumber(potenciaHp, 1.0..2000.0, VehicleFormField.POTENCIA, "Entre 1 y 2000 hp", errors)
    val seconds = optionalNumber(aceleracionS, 1.0..60.0, VehicleFormField.ACELERACION, "Entre 1 y 60 segundos", errors)
    val kmh = optionalNumber(velocidadKmh, 20.0..500.0, VehicleFormField.VELOCIDAD, "Entre 20 y 500 km/h", errors)

    if (errors.isNotEmpty()) return VehicleFormValidation(draft = null, errors = errors)

    return VehicleFormValidation(
        draft = VehicleDraft(
            brand = cleanBrand,
            model = cleanModel,
            year = parsedYear!!,
            category = category!!,
            potencia = hp?.let { "${it.toInt()} hp" },
            aceleracion = seconds?.let { String.format(Locale.US, "%.1f s", it) },
            velocidadMaxima = kmh?.let { "${it.toInt()} km/h" },
            traccion = traccion?.takeIf { it in VEHICLE_TRACTIONS }
        ),
        errors = emptyMap()
    )
}

/** Vacío = no se llenó (válido). Con texto, tiene que ser un número dentro del rango. */
private fun optionalNumber(
    text: String,
    range: ClosedFloatingPointRange<Double>,
    field: VehicleFormField,
    message: String,
    errors: MutableMap<VehicleFormField, String>
): Double? {
    val clean = text.trim()
    if (clean.isEmpty()) return null
    val value = clean.replace(',', '.').toDoubleOrNull()
    if (value == null || value !in range) {
        errors[field] = message
        return null
    }
    return value
}

/**
 * El formulario prellenado con un vehículo ya publicado, para editarlo.
 * Las especificaciones vienen con unidad ("181 hp") y el formulario las
 * quiere como número: se reutiliza el mismo parser del comparador.
 */
fun Vehicle.toFormInput(): VehicleFormInput = VehicleFormInput(
    brand = brand,
    model = model,
    year = year.takeIf { it > 0 }?.toString().orEmpty(),
    category = category.takeIf { it in VEHICLE_CATEGORIES },
    potenciaHp = parseSpecNumber(potencia)?.toInt()?.toString().orEmpty(),
    aceleracionS = parseSpecNumber(aceleracion)?.let { String.format(Locale.US, "%.1f", it) }.orEmpty(),
    velocidadKmh = parseSpecNumber(velocidadMaxima)?.toInt()?.toString().orEmpty(),
    traccion = traccion?.takeIf { it in VEHICLE_TRACTIONS }
)
