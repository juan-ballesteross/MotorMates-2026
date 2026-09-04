package com.example.motormates.ui.search

import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.CarListing
import com.example.motormates.data.model.SearchCategoryKey

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: SearchCategoryKey = SearchCategoryKey.ALL,
    val filteredCars: List<CarListing> = SearchMocks.sampleSearchCars
)
