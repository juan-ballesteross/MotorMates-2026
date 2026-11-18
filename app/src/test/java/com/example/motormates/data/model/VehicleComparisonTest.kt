package com.example.motormates.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * La lógica del comparador en JVM puro: el parser de especificaciones,
 * quién gana cada fila y el largo de las barras.
 */
class VehicleComparisonTest {

    // ---------- parseSpecNumber ----------

    @Test
    fun `parseSpecNumber saca el numero de una spec con unidad`() {
        assertThat(parseSpecNumber("502 hp")).isEqualTo(502.0)
        assertThat(parseSpecNumber("3.4 s")).isEqualTo(3.4)
        assertThat(parseSpecNumber("318 km/h")).isEqualTo(318.0)
    }

    @Test
    fun `parseSpecNumber acepta coma decimal y separador de miles`() {
        assertThat(parseSpecNumber("3,4 s")).isEqualTo(3.4)
        assertThat(parseSpecNumber("1,200 hp")).isEqualTo(1200.0)
    }

    @Test
    fun `parseSpecNumber devuelve null cuando no hay numero`() {
        assertThat(parseSpecNumber(null)).isNull()
        assertThat(parseSpecNumber("")).isNull()
        assertThat(parseSpecNumber("—")).isNull()
        assertThat(parseSpecNumber("RWD")).isNull()
    }

    // ---------- compareNumbers ----------

    @Test
    fun `gana el valor mas alto cuando mas es mejor`() {
        val row = compareNumbers("Potencia", "502 hp", "409 hp", 502.0, 409.0, higherIsBetter = true)

        assertThat(row.winner).isEqualTo(SpecWinner.LEFT)
        assertThat(row.leftFraction).isEqualTo(1f)
        assertThat(row.rightFraction).isWithin(0.001f).of(409f / 502f)
    }

    @Test
    fun `gana el valor mas bajo cuando menos es mejor y su barra queda llena`() {
        val row = compareNumbers("0-100 km/h", "6.7 s", "3.4 s", 6.7, 3.4, higherIsBetter = false)

        assertThat(row.winner).isEqualTo(SpecWinner.RIGHT)
        assertThat(row.rightFraction).isEqualTo(1f)
        assertThat(row.leftFraction).isWithin(0.001f).of((3.4 / 6.7).toFloat())
    }

    @Test
    fun `valores iguales son empate`() {
        val row = compareNumbers("Año", "2024", "2024", 2024.0, 2024.0, higherIsBetter = true)

        assertThat(row.winner).isEqualTo(SpecWinner.TIE)
        assertThat(row.leftFraction).isEqualTo(1f)
        assertThat(row.rightFraction).isEqualTo(1f)
    }

    @Test
    fun `si falta un dato no hay ganador`() {
        val row = compareNumbers("Potencia", "502 hp", "—", 502.0, null, higherIsBetter = true)

        assertThat(row.winner).isEqualTo(SpecWinner.NONE)
        assertThat(row.leftFraction).isEqualTo(1f)
        assertThat(row.rightFraction).isEqualTo(0f)
    }

    // ---------- buildVehicleComparison ----------

    private val mustang = vehicle("mustang", "Ford", "Mustang GT", 2024, "502 hp", "3.4 s", "318 km/h", "RWD")
    private val wrangler = vehicle("wrangler", "Jeep", "Wrangler", 2023, "285 hp", "6.9 s", "180 km/h", "4x4")

    @Test
    fun `el carro con mejores specs gana el veredicto`() {
        val comparison = buildVehicleComparison(mustang, emptyList(), wrangler, emptyList())

        // Potencia, 0-100, vel. máxima y año para el Mustang. La tracción no
        // suma y sin reseñas la calificación no tiene ganador; "Reseñas" 0-0 es empate.
        assertThat(comparison.leftWins).isEqualTo(4)
        assertThat(comparison.rightWins).isEqualTo(0)
        assertThat(comparison.overallWinner).isEqualTo(SpecWinner.LEFT)
    }

    @Test
    fun `la traccion se muestra pero no suma puntos`() {
        val comparison = buildVehicleComparison(mustang, emptyList(), wrangler, emptyList())

        val traccion = comparison.specRows.single { it.label == "Tracción" }
        assertThat(traccion.winner).isEqualTo(SpecWinner.NONE)
        assertThat(traccion.showBars).isFalse()
        assertThat(traccion.leftText).isEqualTo("RWD")
        assertThat(traccion.rightText).isEqualTo("4x4")
    }

    @Test
    fun `la comunidad compara calificacion promedio y numero de resenas`() {
        val leftReviews = listOf(review("1", "mustang", 3), review("2", "mustang", 4))
        val rightReviews = listOf(review("3", "wrangler", 5))

        val comparison = buildVehicleComparison(mustang, leftReviews, wrangler, rightReviews)

        val rating = comparison.communityRows.single { it.label == "Calificación" }
        assertThat(rating.winner).isEqualTo(SpecWinner.RIGHT)
        assertThat(rating.leftText).isEqualTo("3.5 ★")
        assertThat(rating.rightText).isEqualTo("5.0 ★")

        val count = comparison.communityRows.single { it.label == "Reseñas" }
        assertThat(count.winner).isEqualTo(SpecWinner.LEFT)
    }

    @Test
    fun `sin resenas la calificacion dice Sin resenas y no tiene ganador`() {
        val comparison = buildVehicleComparison(mustang, emptyList(), wrangler, listOf(review("1", "wrangler", 4)))

        val rating = comparison.communityRows.single { it.label == "Calificación" }
        assertThat(rating.leftText).isEqualTo("Sin reseñas")
        assertThat(rating.winner).isEqualTo(SpecWinner.NONE)
        assertThat(comparison.left.calificacion).isNull()
    }

    @Test
    fun `la resena destacada es la de mas likes y con comentario`() {
        val reviews = listOf(
            review("1", "mustang", 5, likes = 2, comment = "Buena"),
            review("2", "mustang", 4, likes = 9, comment = "La mejor"),
            review("3", "mustang", 5, likes = 50, comment = "   ")
        )

        val car = mustang.toCompareCarUi(reviews)

        assertThat(car.topReview?.comentario).isEqualTo("La mejor")
        assertThat(car.topReview?.likesCount).isEqualTo(9)
    }

    @Test
    fun `specs faltantes se muestran con guion y no dan ganador`() {
        val sinDatos = vehicle("x", "Marca", "Modelo", 2020, null, null, null, null)

        val comparison = buildVehicleComparison(mustang, emptyList(), sinDatos, emptyList())

        val potencia = comparison.specRows.single { it.label == "Potencia" }
        assertThat(potencia.rightText).isEqualTo("—")
        assertThat(potencia.winner).isEqualTo(SpecWinner.NONE)
    }

    // ---------- myCompareVehicleIds / garageOf ----------

    @Test
    fun `mis carros son primero los propios y luego los resenados sin repetir`() {
        val vehicles = listOf(mustang.copy(ownerId = "me"), wrangler.copy(ownerId = "otro"))
        val myReviews = listOf(review("1", "tesla", 5), review("2", "mustang", 4), review("3", "tesla", 3))

        val ids = myCompareVehicleIds(vehicles, myReviews, myUid = "me", excludeId = null)

        assertThat(ids).containsExactly("mustang", "tesla").inOrder()
    }

    @Test
    fun `mis carros no incluyen el carro contra el que se compara`() {
        val ids = myCompareVehicleIds(
            vehicles = listOf(mustang.copy(ownerId = "me")),
            myReviews = listOf(review("1", "wrangler", 5)),
            myUid = "me",
            excludeId = "wrangler"
        )

        assertThat(ids).containsExactly("mustang")
    }

    @Test
    fun `el garaje solo trae los carros a nombre del usuario`() {
        val vehicles = listOf(
            wrangler.copy(ownerId = "sofia"),
            mustang.copy(ownerId = "sofia"),
            vehicle("x", "Tesla", "Model 3", 2024, null, null, null, null).copy(ownerId = "ivan")
        )

        val garage = vehicles.garageOf("sofia")

        assertThat(garage.map { it.title }).containsExactly("Ford Mustang GT", "Jeep Wrangler").inOrder()
        assertThat(garage.first().subtitle).isEqualTo("2024 · Cat")
    }

    private fun vehicle(
        id: String, brand: String, model: String, year: Int,
        potencia: String?, aceleracion: String?, velocidad: String?, traccion: String?
    ) = Vehicle(
        id = id, brand = brand, model = model, year = year, category = "Cat", imageUrl = null,
        potencia = potencia, aceleracion = aceleracion, velocidadMaxima = velocidad, traccion = traccion
    )

    private fun review(id: String, vehicleId: String, rating: Int, likes: Int = 0, comment: String = "ok") = Review(
        id = id, rating = rating, comment = comment, userId = "u$id", vehicleId = vehicleId,
        createdAt = null, likesCount = likes, author = ReviewAuthor("u$id", "Autor $id"), vehicle = null
    )
}
