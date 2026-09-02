package com.example.motormates.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.motormates.data.model.CarListing
import com.example.motormates.data.model.SearchCategory
import com.example.motormates.data.model.SearchCategoryKey
import com.example.motormates.ui.search.components.CarResultsGrid
import com.example.motormates.ui.search.components.CategoryChipsRow
import com.example.motormates.ui.search.components.EmptySearchState

@Composable
fun SearchScreenContent(
    categories: List<SearchCategory>,
    selectedCategory: SearchCategoryKey,
    onSelectCategory: (SearchCategoryKey) -> Unit,
    filteredCars: List<CarListing>,
    onCarClick: (CarListing) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        CategoryChipsRow(
            categories = categories,
            selected = selectedCategory,
            onSelect = onSelectCategory
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (filteredCars.isEmpty()) {
            EmptySearchState()
        } else {
            CarResultsGrid(cars = filteredCars, onCarClick = onCarClick)
        }
    }
}
