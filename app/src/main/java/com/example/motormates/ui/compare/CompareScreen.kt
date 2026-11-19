package com.example.motormates.ui.compare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.compare.components.VehiclePickerSheet

/**
 * Comparador de dos carros. rightVehicleId es opcional: entrando desde el
 * detalle solo se conoce el primero y la pantalla abre el selector para
 * elegir el segundo. Las otras entradas (Explorar, perfil) pueden pasar los dos.
 */
@Composable
fun CompareScreen(
    leftVehicleId: String,
    rightVehicleId: String?,
    compareWithMine: Boolean = false,
    onBackClick: () -> Unit,
    onVehicleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(leftVehicleId, rightVehicleId, compareWithMine) {
        viewModel.start(leftVehicleId, rightVehicleId, compareWithMine)
    }

    CompareScreenContent(
        state = state,
        onBackClick = onBackClick,
        onVehicleClick = onVehicleClick,
        onChangeVehicleClick = viewModel::openPicker,
        onRetryClick = viewModel::retry,
        modifier = modifier
    )

    if (state.picker.isOpen) {
        VehiclePickerSheet(
            picker = state.picker,
            onQueryChange = viewModel::onPickerQueryChange,
            onOwnerSelected = viewModel::onPickerOwnerSelected,
            onBackToOwners = viewModel::onPickerBack,
            onVehicleSelected = viewModel::selectVehicle,
            onRetryClick = viewModel::loadCandidates,
            onDismiss = viewModel::dismissPicker
        )
    }
}
