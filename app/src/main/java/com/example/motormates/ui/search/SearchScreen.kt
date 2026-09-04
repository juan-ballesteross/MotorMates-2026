package com.example.motormates.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.data.model.CarListing
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.search.components.SearchTopBar
import com.example.motormates.ui.search.model.rememberSearchCategories
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Ya NO tiene su propio Scaffold. searchQuery/selectedCategory viven en
 * SearchViewModel como StateFlow (el topBar de
 * búsqueda vive aquí mismo, no en el Scaffold central).
 */
@Composable
fun SearchScreen(
    onCarClick: (CarListing) -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val categories = rememberSearchCategories()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SearchTopBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery
        )
        SearchScreenContent(
            categories = categories,
            selectedCategory = uiState.selectedCategory,
            onSelectCategory = viewModel::updateSelectedCategory,
            filteredCars = uiState.filteredCars,
            onCarClick = onCarClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.EXPLORE) }
        ) { innerPadding ->
            SearchScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
