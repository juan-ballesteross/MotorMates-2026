package com.example.motormates.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.mock.SearchMocks
import com.example.motormates.data.model.SearchCategoryKey
import com.example.motormates.data.model.toUserSearchResult
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    init {
        _uiState.update { it.copy(filteredCars = SearchMocks.sampleSearchCars) }
        loadUsers()
    }

    /** Usuarios reales registrados con Firebase, para el buscador. */
    private fun loadUsers() {
        viewModelScope.launch {
            val currentUid = authRepository.getCurrentUser()?.id
            userRepository.getAllRegisteredUsers().onSuccess { users ->
                val results = users
                    .filter { it.id != currentUid }
                    .map { it.toUserSearchResult() }
                _uiState.update { it.copy(allUsers = results).filterUsers() }
            }
        }
    }

    fun updateSearchQuery(input: String) {
        val wasBlank = _uiState.value.searchQuery.isBlank()

        _uiState.update { current ->
            current.copy(searchQuery = input).filterCars().filterUsers()
        }

        if (wasBlank && input.isNotBlank()) {
            loadUsers()
        }
    }

    fun updateSelectedCategory(category: SearchCategoryKey) {
        _uiState.update { current ->
            current.copy(selectedCategory = category).filterCars()
        }
    }

    private fun SearchUiState.filterCars(): SearchUiState {
        val filtered = filteredCars.filter { car ->
            val matchesCategory = selectedCategory == SearchCategoryKey.ALL || car.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                car.brand.contains(searchQuery, ignoreCase = true) ||
                car.model.contains(searchQuery, ignoreCase = true) ||
                car.ownerName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        return copy(filteredCars = filtered)
    }

    /**
     * A diferencia de filterCars (que filtra sobre su propio resultado
     * anterior), esto siempre filtra desde allUsers, la lista completa.
     */
    private fun SearchUiState.filterUsers(): SearchUiState {
        val results = if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allUsers.filter { user ->
                user.name.contains(searchQuery, ignoreCase = true) ||
                    user.handle.contains(searchQuery, ignoreCase = true)
            }
        }
        return copy(userResults = results)
    }
}
