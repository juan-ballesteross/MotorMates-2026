package com.example.motormates.ui.carDetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de la pantalla. Aquí sí vive el estado (isBookmarked),
 * y se pasa hacia abajo al composable stateless CarDetailScreenContent.
 * Reemplaza car/reviews por los datos reales cuando conectes la API/Firebase.
 */
@Composable
fun CarDetailScreen(
    car: CarDetailUi = mockCarDetail,
    reviews: List<ReviewUi> = mockReviews,
    onBackClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(false) }

    CarDetailScreenContent(
        car = car,
        reviews = reviews,
        isBookmarked = isBookmarked,
        onBackClick = onBackClick,
        onBookmarkClick = { isBookmarked = !isBookmarked },
        onWriteReviewClick = onWriteReviewClick,
        onSeeAllReviewsClick = onSeeAllReviewsClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun CarDetailScreenPreview() {
    MotorMatesTheme {
        CarDetailScreen()
    }
}