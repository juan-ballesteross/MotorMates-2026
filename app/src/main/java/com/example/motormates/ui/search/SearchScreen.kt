package com.example.motormates.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.motormates.ui.search.components.SearchTopBar
import com.example.motormates.ui.search.model.CarListing
import com.example.motormates.ui.search.model.SearchCategoryKey
import com.example.motormates.ui.search.model.rememberSearchCategories
import com.example.motormates.ui.search.model.sampleSearchCars
import com.example.motormates.ui.theme.MotorMatesBackground

/**
 * Ya NO tiene su propio Scaffold. searchQuery/selectedCategory se quedan
 * como estado local porque solo los usa esta pantalla (el topBar de
 * búsqueda vive aquí mismo, no en el Scaffold central).
 */
@Composable
fun SearchScreen(
    onCarClick: (CarListing) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategoryKey.ALL) }

    val categories = rememberSearchCategories()

    val filteredCars = sampleSearchCars.filter { car ->
        val matchesCategory = selectedCategory == SearchCategoryKey.ALL || car.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                car.brand.contains(searchQuery, ignoreCase = true) ||
                car.model.contains(searchQuery, ignoreCase = true) ||
                car.ownerName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
    ) {
        SearchTopBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )
        SearchScreenContent(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            filteredCars = filteredCars,
            onCarClick = onCarClick
        )
    }
}
