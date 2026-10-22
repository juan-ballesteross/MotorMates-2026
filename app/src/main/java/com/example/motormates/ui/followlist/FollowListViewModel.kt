package com.example.motormates.ui.followlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toUserSearchResult
import com.example.motormates.data.repository.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState

    fun load(uid: String, isFollowers: Boolean) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (isFollowers) {
                followRepository.getFollowers(uid)
            } else {
                followRepository.getFollowing(uid)
            }

            result.fold(
                onSuccess = { users ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            users = users.map { user -> user.toUserSearchResult() }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo cargar la lista"
                        )
                    }
                }
            )
        }
    }
}
