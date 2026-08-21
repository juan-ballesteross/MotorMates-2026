package com.example.motormates.ui.carDetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.data.mock.CarDetailMocks
import com.example.motormates.data.model.CarDetailUi
import com.example.motormates.data.model.ReviewUi
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Punto de entrada de la pantalla. Aquí sí vive el estado (isBookmarked)
 * y la pantalla busca sus propios datos (car/reviews) en la capa data,
 * en vez de recibirlos por parámetro. Reemplaza CarDetailMocks
 * por una llamada real (repositorio/ViewModel) cuando exista la API/Firebase.
 */
@Composable
fun CarDetailScreen(
    onBackClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(false) }
    val car: CarDetailUi = CarDetailMocks.mockCarDetail
    val reviews: List<ReviewUi> = CarDetailMocks.mockReviews

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
