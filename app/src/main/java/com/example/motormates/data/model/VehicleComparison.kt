package com.example.motormates.data.model

import java.util.Locale
import kotlin.math.abs

/**
 * Comparación lado a lado de dos vehículos.
 *
 * Toda la lógica vive acá, en Kotlin puro (sin Android), para poder
 * probarla con JUnit en la JVM: qué número sale de "502 hp", quién gana
 * cada fila y qué tan larga es cada barra. La UI solo pinta el resultado.
 */

/** Lado de la comparación. La izquierda es el carro desde el que se entró. */
enum class CompareSide { LEFT, RIGHT }

/** Quién gana una fila. NONE = no se puede decidir (falta un dato o no es comparable). */
enum class SpecWinner { LEFT, RIGHT, TIE, NONE }

/** Un carro tal como se muestra en el comparador. */
data class CompareCarUi(
    val id: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val categoria: String,
    val imageUrl: String?,
    /** Promedio de estrellas; null si todavía no tiene reseñas. */
    val calificacion: Float?,
    val numeroResenas: Int,
    /** La reseña con más likes, para la sección de comunidad. */
    val topReview: CompareTopReviewUi?
)

data class CompareTopReviewUi(
    val autor: String,
    val comentario: String,
    val calificacion: Int,
    val likesCount: Int
)

/**
 * Una fila de la tabla. leftFraction/rightFraction van de 0 a 1 y son el
 * largo de cada barra: el ganador siempre llena la suya, sea porque tiene
 * el número más alto (potencia) o el más bajo (0-100).
 */
data class SpecComparisonRow(
    val label: String,
    val leftText: String,
    val rightText: String,
    val winner: SpecWinner,
    val leftFraction: Float = 0f,
    val rightFraction: Float = 0f,
    /** false para filas de texto como la tracción, que no tienen barra. */
    val showBars: Boolean = true
)

data class VehicleComparison(
    val left: CompareCarUi,
    val right: CompareCarUi,
    val specRows: List<SpecComparisonRow>,
    val communityRows: List<SpecComparisonRow>
) {
    private val allRows: List<SpecComparisonRow> get() = specRows + communityRows

    val leftWins: Int get() = allRows.count { it.winner == SpecWinner.LEFT }
    val rightWins: Int get() = allRows.count { it.winner == SpecWinner.RIGHT }

    /** Veredicto general: quién ganó más filas. */
    val overallWinner: SpecWinner
        get() = when {
            leftWins == 0 && rightWins == 0 -> SpecWinner.NONE
            leftWins > rightWins -> SpecWinner.LEFT
            rightWins > leftWins -> SpecWinner.RIGHT
            else -> SpecWinner.TIE
        }
}

private const val MISSING = "—"

/**
 * Saca el número de una especificación que viene con su unidad:
 * "502 hp" -> 502.0, "3.4 s" -> 3.4, "3,4 s" -> 3.4, "1,200 hp" -> 1200.0.
 * Devuelve null si no hay número ("—", vacío o null).
 */
fun parseSpecNumber(text: String?): Double? {
    if (text.isNullOrBlank()) return null
    // Primero se quitan los separadores de miles ("1,200" -> "1200") y
    // después la coma que quede se toma como decimal ("3,4" -> "3.4").
    val normalized = THOUSANDS_SEPARATOR.replace(text, "$1$2")
    val match = NUMBER.find(normalized) ?: return null
    return match.value.replace(',', '.').toDoubleOrNull()
}

private val THOUSANDS_SEPARATOR = Regex("""(\d),(\d{3})(?!\d)""")
private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")

/**
 * Compara dos valores numéricos. higherIsBetter = false para cosas como el
 * 0-100, donde gana el que tarda menos.
 */
internal fun compareNumbers(
    label: String,
    leftText: String,
    rightText: String,
    leftValue: Double?,
    rightValue: Double?,
    higherIsBetter: Boolean
): SpecComparisonRow {
    if (leftValue == null || rightValue == null) {
        // Sin los dos datos no hay ganador; el que sí tiene dato muestra
        // su barra completa para que no parezca un cero.
        return SpecComparisonRow(
            label = label,
            leftText = leftText,
            rightText = rightText,
            winner = SpecWinner.NONE,
            leftFraction = if (leftValue != null) 1f else 0f,
            rightFraction = if (rightValue != null) 1f else 0f
        )
    }

    val winner = when {
        abs(leftValue - rightValue) < EPSILON -> SpecWinner.TIE
        (leftValue > rightValue) == higherIsBetter -> SpecWinner.LEFT
        else -> SpecWinner.RIGHT
    }

    return SpecComparisonRow(
        label = label,
        leftText = leftText,
        rightText = rightText,
        winner = winner,
        leftFraction = barFraction(leftValue, leftValue, rightValue, higherIsBetter),
        rightFraction = barFraction(rightValue, leftValue, rightValue, higherIsBetter)
    )
}

private fun barFraction(value: Double, a: Double, b: Double, higherIsBetter: Boolean): Float {
    if (value <= 0.0) return 0f
    val fraction = if (higherIsBetter) value / maxOf(a, b) else minOf(a, b) / value
    return fraction.toFloat().coerceIn(0f, 1f)
}

private const val EPSILON = 1e-9

fun Vehicle.toCompareCarUi(reviews: List<Review>): CompareCarUi = CompareCarUi(
    id = id,
    marca = brand.uppercase(),
    modelo = model,
    anio = year,
    categoria = category,
    imageUrl = imageUrl,
    calificacion = if (reviews.isEmpty()) null else reviews.map { it.rating }.average().toFloat(),
    numeroResenas = reviews.size,
    topReview = reviews
        .filter { !it.comment.isNullOrBlank() }
        .maxWithOrNull(compareBy<Review>({ it.likesCount }, { it.rating }))
        ?.let { review ->
            CompareTopReviewUi(
                autor = review.author?.fullName?.ifBlank { null } ?: "Usuario",
                comentario = review.comment.orEmpty(),
                calificacion = review.rating,
                likesCount = review.likesCount
            )
        }
)

/** Arma la comparación completa a partir de los dos vehículos y sus reseñas. */
fun buildVehicleComparison(
    left: Vehicle,
    leftReviews: List<Review>,
    right: Vehicle,
    rightReviews: List<Review>
): VehicleComparison {
    val leftUi = left.toCompareCarUi(leftReviews)
    val rightUi = right.toCompareCarUi(rightReviews)

    val specRows = listOf(
        compareNumbers(
            label = "Potencia",
            leftText = left.potencia ?: MISSING,
            rightText = right.potencia ?: MISSING,
            leftValue = parseSpecNumber(left.potencia),
            rightValue = parseSpecNumber(right.potencia),
            higherIsBetter = true
        ),
        compareNumbers(
            label = "0-100 km/h",
            leftText = left.aceleracion ?: MISSING,
            rightText = right.aceleracion ?: MISSING,
            leftValue = parseSpecNumber(left.aceleracion),
            rightValue = parseSpecNumber(right.aceleracion),
            higherIsBetter = false
        ),
        compareNumbers(
            label = "Vel. máxima",
            leftText = left.velocidadMaxima ?: MISSING,
            rightText = right.velocidadMaxima ?: MISSING,
            leftValue = parseSpecNumber(left.velocidadMaxima),
            rightValue = parseSpecNumber(right.velocidadMaxima),
            higherIsBetter = true
        ),
        compareNumbers(
            label = "Año",
            leftText = left.year.toString(),
            rightText = right.year.toString(),
            leftValue = left.year.toDouble(),
            rightValue = right.year.toDouble(),
            higherIsBetter = true
        ),
        // La tracción no es "mejor" o "peor": se muestra, pero no suma puntos.
        SpecComparisonRow(
            label = "Tracción",
            leftText = left.traccion ?: MISSING,
            rightText = right.traccion ?: MISSING,
            winner = SpecWinner.NONE,
            showBars = false
        )
    )

    val communityRows = listOf(
        compareNumbers(
            label = "Calificación",
            leftText = leftUi.calificacion.toRatingText(),
            rightText = rightUi.calificacion.toRatingText(),
            leftValue = leftUi.calificacion?.toDouble(),
            rightValue = rightUi.calificacion?.toDouble(),
            higherIsBetter = true
        ),
        compareNumbers(
            label = "Reseñas",
            leftText = leftUi.numeroResenas.toString(),
            rightText = rightUi.numeroResenas.toString(),
            leftValue = leftUi.numeroResenas.toDouble(),
            rightValue = rightUi.numeroResenas.toDouble(),
            higherIsBetter = true
        )
    )

    return VehicleComparison(
        left = leftUi,
        right = rightUi,
        specRows = specRows,
        communityRows = communityRows
    )
}

private fun Float?.toRatingText(): String =
    if (this == null) "Sin reseñas" else String.format(Locale.US, "%.1f ★", this)

/**
 * Qué carros cuentan como "míos" para "Comparar con el mío", en orden de
 * prioridad: primero los que tengo a mi nombre (ownerId) y después los que
 * he reseñado, porque hoy la app no le asigna carros a los usuarios reales
 * y sin eso la lista casi siempre saldría vacía. Sin repetidos y sin el
 * carro contra el que se va a comparar.
 */
fun myCompareVehicleIds(
    vehicles: List<Vehicle>,
    myReviews: List<Review>,
    myUid: String,
    excludeId: String?
): List<String> {
    val owned = vehicles.filter { it.ownerId == myUid }.map { it.id }
    val reviewed = myReviews.map { it.vehicleId }
    return (owned + reviewed).distinct().filter { it != excludeId }
}
