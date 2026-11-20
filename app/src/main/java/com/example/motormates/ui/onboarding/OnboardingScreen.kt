package com.example.motormates.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.reviewsMap.components.LocationPermissionDialogs
import com.example.motormates.ui.reviewsMap.components.rememberLocationPermissionHandler

/**
 * editMode = false: viene del login, del registro o del splash; si el
 * usuario ya contestó antes, onFinished se llama de una vez.
 * editMode = true: "Editar preferencias" desde el perfil.
 */
@Composable
fun OnboardingScreen(
    editMode: Boolean,
    onFinished: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(editMode) { viewModel.load(editMode) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }

    // El mismo manejo de permisos que el botón "Mi ubicación" del mapa.
    val locationPermission = rememberLocationPermissionHandler(
        onAccessChanged = {},
        onReady = viewModel::detectCity
    )
    LocationPermissionDialogs(locationPermission)

    // Atrás retrocede un paso; en el primero, al editar, cierra sin guardar.
    BackHandler(enabled = !state.isFirstStep || state.isEditMode) {
        if (state.isFirstStep) onClose() else viewModel.back()
    }

    OnboardingContent(
        state = state,
        actions = OnboardingActions(
            onNext = viewModel::next,
            onBack = viewModel::back,
            onSkip = viewModel::skip,
            onClose = onClose,
            onToggleCategory = viewModel::toggleCategory,
            onBrandQueryChange = viewModel::updateBrandQuery,
            onToggleBrand = viewModel::toggleBrand,
            onHasCarChange = viewModel::setHasCar,
            onCarBrandChange = viewModel::updateCarBrand,
            onCarModelChange = viewModel::updateCarModel,
            onCarYearChange = viewModel::updateCarYear,
            onCarCategoryChange = viewModel::selectCarCategory,
            onToggleInterest = viewModel::toggleInterest,
            onSelectCity = viewModel::selectCity,
            onUseMyLocation = locationPermission::request,
            onToggleFollow = viewModel::toggleFollow,
            onFollowAll = viewModel::followAll
        ),
        modifier = modifier
    )
}
