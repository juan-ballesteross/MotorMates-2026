package com.example.motormates.ui.vehicleDetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.data.mock.CarDetailMocks
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.toCarDetailUi
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.vehicleDetail.components.VehicleDetailNotFound

/**
 * Punto de entrada de la pantalla de detalle de vehículo. Recibe el
 * vehicleId ya resuelto por AppNavigation y busca el vehículo directamente
 * en los mocks, igual que el resto de pantallas del proyecto.
 */
@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    onBackClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(false) }
    val car = remember(vehicleId) { SearchMocks.findById(vehicleId)?.toCarDetailUi() }

    if (car == null) {
        VehicleDetailNotFound(onBackClick = onBackClick, modifier = modifier)
    } else {
        VehicleDetailContent(
            car = car,
            reviews = CarDetailMocks.mockReviews,
            isBookmarked = isBookmarked,
            onBackClick = onBackClick,
            onBookmarkClick = { isBookmarked = !isBookmarked },
            onWriteReviewClick = onWriteReviewClick,
            onSeeAllReviewsClick = onSeeAllReviewsClick,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailNotFoundPreview() {
    MotorMatesTheme {
        VehicleDetailNotFound(onBackClick = {})
    }
}
