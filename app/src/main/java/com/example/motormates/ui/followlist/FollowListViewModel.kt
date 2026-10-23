package com.example.motormates.ui.followlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toUserSearchResult
import com.example.motormates.data.repository.FollowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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

    private var usersJob: Job? = null

    /**
     * La lista queda escuchándose: si alguien empieza a seguir a este
     * usuario mientras la pantalla está abierta, aparece sola.
     */
    fun load(uid: String, isFollowers: Boolean) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val users = if (isFollowers) {
            followRepository.observeFollowers(uid)
        } else {
            followRepository.observeFollowing(uid)
        }

        usersJob?.cancel()
        usersJob = viewModelScope.launch {
            users.collect { result ->
                result.fold(
                    onSuccess = { followList ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null,
                                users = followList.map { user -> user.toUserSearchResult() }
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
}
