package com.example.motormates.ui.search

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.search.components.SearchTopBar
import com.example.motormates.ui.search.model.SearchCategoryKey
import com.example.motormates.ui.search.model.rememberSearchCategories
import com.example.motormates.ui.search.model.sampleSearchCars
import com.example.motormates.ui.theme.MotorMatesBackground

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onFeedClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
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

    Scaffold(
        modifier = modifier,
        containerColor = MotorMatesBackground,
        topBar = {
            SearchTopBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        },
        bottomBar = {
            MainBottomNavBar(
                selected = MainBottomDestination.EXPLORE,
                onFeedClick = onFeedClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        SearchScreenContent(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            filteredCars = filteredCars,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
