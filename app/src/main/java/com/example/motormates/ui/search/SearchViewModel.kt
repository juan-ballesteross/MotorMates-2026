package com.example.motormates.ui.search

import androidx.lifecycle.ViewModel
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.SearchCategoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SearchViewModel : ViewModel() {

    private val cars = SearchMocks.sampleSearchCars
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    init {
        _uiState.update { it.copy(filteredCars = cars) }
    }

    fun updateSearchQuery(input: String) {
        _uiState.update { current ->
            current.copy(searchQuery = input).filterCars()
        }
    }

    fun updateSelectedCategory(category: SearchCategoryKey) {
        _uiState.update { current ->
            current.copy(selectedCategory = category).filterCars()
        }
    }

    private fun SearchUiState.filterCars(): SearchUiState {
        val filtered = cars.filter { car ->
            val matchesCategory = selectedCategory == SearchCategoryKey.ALL || car.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                car.brand.contains(searchQuery, ignoreCase = true) ||
                car.model.contains(searchQuery, ignoreCase = true) ||
                car.ownerName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        return copy(filteredCars = filtered)
    }
}
