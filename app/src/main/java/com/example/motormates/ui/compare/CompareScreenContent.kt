package com.example.motormates.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.CompareSide
import com.example.motormates.data.model.Review
import com.example.motormates.data.model.ReviewAuthor
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.model.buildVehicleComparison
import com.example.motormates.ui.compare.components.CompareHeader
import com.example.motormates.ui.compare.components.CompareSectionTitle
import com.example.motormates.ui.compare.components.CompareTopBar
import com.example.motormates.ui.compare.components.SpecBarRow
import com.example.motormates.ui.compare.components.TopReviewsSection
import com.example.motormates.ui.compare.components.VerdictCard
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Spacing

/** Stateless: todo llega por parámetro (state hoisting), igual que el resto de pantallas. */
@Composable
fun CompareScreenContent(
    state: CompareState,
    onBackClick: () -> Unit,
    onVehicleClick: (String) -> Unit,
    onChangeVehicleClick: (CompareSide) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("compareScreen")
    ) {
        CompareTopBar(onBackClick = onBackClick)

        // Al cambiar uno de los carros la comparación vieja se queda en
        // pantalla y arriba se muestra esta barra mientras llega la nueva.
        if (state.isLoading && state.left != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        when {
            state.left == null && state.errorMessage != null -> ErrorMessage(
                message = state.errorMessage,
                onRetryClick = onRetryClick,
                modifier = Modifier.fillMaxSize()
            )

            state.left == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.gutter,
                    end = Spacing.gutter,
                    bottom = Spacing.lg
                )
            ) {
                item {
                    CompareHeader(
                        left = state.left,
                        right = state.right,
                        onCarClick = onVehicleClick,
                        onChangeClick = onChangeVehicleClick
                    )
                }

                if (state.errorMessage != null) {
                    item {
                        ErrorMessage(message = state.errorMessage, onRetryClick = onRetryClick)
                    }
                }

                val comparison = state.comparison
                if (comparison == null) {
                    item {
                        if (state.isFindingMyCar) {
                            Text(
                                text = "Buscando tu carro…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.xl)
                            )
                        } else if (!state.isLoading) {
                            Text(
                                text = "Elige un segundo carro para ver quién gana en potencia, aceleración, velocidad y en la opinión de la comunidad.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.xl)
                            )
                        }
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        VerdictCard(comparison = comparison)
                    }

                    item { CompareSectionTitle("Especificaciones") }
                    items(comparison.specRows, key = { "spec_${it.label}" }) { row ->
                        SpecBarRow(row = row)
                    }

                    item { CompareSectionTitle("Comunidad") }
                    items(comparison.communityRows, key = { "community_${it.label}" }) { row ->
                        SpecBarRow(row = row)
                    }

                    item {
                        CompareSectionTitle("Reseña más popular")
                        TopReviewsSection(left = comparison.left, right = comparison.right)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onRetryClick) { Text("Reintentar") }
    }
}

// ---------- Previews ----------

private val previewLeft = Vehicle(
    id = "mustang", brand = "Ford", model = "Mustang GT", year = 2024, category = "Deportivo",
    imageUrl = null, potencia = "502 hp", aceleracion = "3.4 s", velocidadMaxima = "318 km/h", traccion = "RWD"
)
private val previewRight = Vehicle(
    id = "wrangler", brand = "Jeep", model = "Wrangler", year = 2023, category = "4x4",
    imageUrl = null, potencia = "285 hp", aceleracion = "6.9 s", velocidadMaxima = "180 km/h", traccion = "4x4"
)
private fun previewReview(id: String, vehicleId: String, rating: Int, likes: Int) = Review(
    id = id, rating = rating, comment = "Una máquina, el sonido del motor no tiene comparación.",
    userId = "u$id", vehicleId = vehicleId, createdAt = null, likesCount = likes,
    author = ReviewAuthor(id = "u$id", fullName = "Diego Melgarejo"), vehicle = null
)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun CompareScreenContentPreview() {
    val leftReviews = listOf(previewReview("1", "mustang", 5, 12), previewReview("2", "mustang", 4, 3))
    val rightReviews = listOf(previewReview("3", "wrangler", 4, 7))
    val comparison = buildVehicleComparison(previewLeft, leftReviews, previewRight, rightReviews)
    MotorMatesTheme {
        CompareScreenContent(
            state = CompareState(
                leftId = "mustang",
                rightId = "wrangler",
                left = comparison.left,
                right = comparison.right,
                comparison = comparison
            ),
            onBackClick = {},
            onVehicleClick = {},
            onChangeVehicleClick = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompareScreenContentOnlyLeftPreview() {
    val comparison = buildVehicleComparison(previewLeft, emptyList(), previewRight, emptyList())
    MotorMatesTheme {
        CompareScreenContent(
            state = CompareState(leftId = "mustang", left = comparison.left),
            onBackClick = {},
            onVehicleClick = {},
            onChangeVehicleClick = {},
            onRetryClick = {}
        )
    }
}
