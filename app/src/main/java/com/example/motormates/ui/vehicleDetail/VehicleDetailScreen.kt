package com.example.motormates.ui.vehicleDetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    onBackClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    viewModel: VehicleDetailViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()

    LaunchedEffect(vehicleId) {
        viewModel.getVehicleById(vehicleId)
        viewModel.getReviews(vehicleId)
    }

    val vehicle = state.vehicle
    if (vehicle == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Vehículo no encontrado", color = MaterialTheme.colorScheme.onBackground)
        }
    } else {
        VehicleDetailContent(
            car = vehicle,
            reviews = state.reviews,
            isBookmarked = isBookmarked,
            onBackClick = onBackClick,
            onBookmarkClick = viewModel::bookmarkButtonPress,
            onWriteReviewClick = onWriteReviewClick,
            onSeeAllReviewsClick = onSeeAllReviewsClick,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailScreenPreview() {
    MotorMatesTheme {
        VehicleDetailScreen(vehicleId = 1)
    }
}
