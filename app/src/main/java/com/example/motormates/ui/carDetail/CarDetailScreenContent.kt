package com.example.motormates.ui.carDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.carDetail.components.CarImageHeader
import com.example.motormates.ui.carDetail.components.RatingRow
import com.example.motormates.ui.carDetail.components.ReviewItem
import com.example.motormates.ui.carDetail.components.SpecStatCard

/**
 * Todo el estado viene del padre (state hoisting) — este composable
 * no guarda nada con remember, solo pinta lo que le llega.
 */
@Composable
fun CarDetailScreenContent(
    car: CarDetailUi,
    reviews: List<ReviewUi>,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onSeeAllReviewsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val specs = listOf(
        car.potencia to "Potencia",
        car.aceleracion to "0-100 km/h",
        car.velocidadMaxima to "Vel. máxima",
        car.traccion to "Tracción"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            CarImageHeader(
                imagenResId = car.imagenResId,
                isBookmarked = isBookmarked,
                onBackClick = onBackClick,
                onBookmarkClick = onBookmarkClick
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = car.marca,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = car.modelo,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                RatingRow(calificacion = car.calificacion, numeroResenas = car.numeroResenas)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            // Requisito del sprint: al menos una pantalla con Lazy — aquí usamos
            // LazyRow para las tarjetas de specs (horizontal, no todas caben en pantalla).
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(specs) { (valor, etiqueta) ->
                    SpecStatCard(valor = valor, etiqueta = etiqueta)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onWriteReviewClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Escribir reseña",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Reseñas de la comunidad",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Ver todos",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Lista de reseñas dentro del MISMO LazyColumn (no se anida otro lazy adentro).
        items(reviews) { review ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ReviewItem(review = review)
            }
        }
    }
}