package com.example.motormates.ui.search

import com.example.motormates.data.model.CarListing
import com.example.motormates.data.model.SearchCategoryKey
import com.example.motormates.data.model.UserSearchResult

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: SearchCategoryKey = SearchCategoryKey.ALL,
    // Catálogo completo. Los filtros siempre parten de aquí: si filtraran
    // sobre filteredCars, al volver a "Todos" no reaparecerían los carros.
    val allCars: List<CarListing> = emptyList(),
    val isLoadingCars: Boolean = false,
    val carsError: String? = null,
    val filteredCars: List<CarListing> = emptyList(),
    // Lista completa de usuarios registrados (Firestore), para filtrar en
    // memoria cada vez que cambia la búsqueda, sin volver a consultarlos.
    val allUsers: List<UserSearchResult> = emptyList(),
    val userResults: List<UserSearchResult> = emptyList()
)
