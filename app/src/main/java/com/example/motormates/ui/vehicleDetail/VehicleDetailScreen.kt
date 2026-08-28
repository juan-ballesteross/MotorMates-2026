package com.example.motormates.ui.vehicleDetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.motormates.data.viewmodel.VehicleDetailUiState
import com.example.motormates.data.viewmodel.VehicleDetailViewModel
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.vehicleDetail.components.VehicleDetailError
import com.example.motormates.ui.vehicleDetail.components.VehicleDetailLoading
import com.example.motormates.ui.vehicleDetail.components.VehicleDetailNotFound

/**
 * Punto de entrada de la pantalla de detalle de vehículo. Recibe el
 * vehicleId ya resuelto por AppNavigation y construye el ViewModel con
 * ese id directamente (sin depender de SavedStateHandle), cubriendo los
 * 4 estados posibles: cargando, encontrado, no encontrado y error.
 */
@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    onBackClick: () -> Unit = {},
    onWriteReviewClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: VehicleDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { VehicleDetailViewModel(vehicleId) } }
    )
) {
    var isBookmarked by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is VehicleDetailUiState.Loading -> VehicleDetailLoading(modifier = modifier)
        is VehicleDetailUiState.NotFound -> VehicleDetailNotFound(onBackClick = onBackClick, modifier = modifier)
        is VehicleDetailUiState.Error -> VehicleDetailError(
            message = state.message,
            onBackClick = onBackClick,
            modifier = modifier
        )
        is VehicleDetailUiState.Success -> VehicleDetailContent(
            car = state.car,
            reviews = state.reviews,
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
private fun VehicleDetailLoadingPreview() {
    MotorMatesTheme {
        VehicleDetailLoading()
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailNotFoundPreview() {
    MotorMatesTheme {
        VehicleDetailNotFound(onBackClick = {})
    }
}
