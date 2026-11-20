package com.example.motormates.ui.buildDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.Build
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.BuildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuildDetailState(
    val isLoading: Boolean = true,
    val build: Build? = null,
    val isMine: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val isUpdating: Boolean = false,
    val confirmDelete: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class BuildDetailViewModel @Inject constructor(
    private val buildRepository: BuildRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BuildDetailState())
    val state: StateFlow<BuildDetailState> = _state.asStateFlow()

    fun load(buildId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            buildRepository.getBuild(buildId)
                .onSuccess { build ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            build = build,
                            isMine = build.ownerId == authRepository.getCurrentUser()?.id
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    /** Solo el dueño publica o retira su build. */
    fun togglePublished() {
        val build = _state.value.build ?: return
        if (!_state.value.isMine || _state.value.isUpdating) return
        val published = !build.published
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true) }
            buildRepository.setPublished(build.id, published)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isUpdating = false,
                            build = build.copy(published = published),
                            message = if (published) "Ahora la comunidad puede ver tu build" else "Tu build ya no es público"
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(isUpdating = false, message = error.message) } }
        }
    }

    fun askDelete() = _state.update { it.copy(confirmDelete = true) }

    fun dismissDelete() = _state.update { it.copy(confirmDelete = false) }

    fun confirmDelete() {
        val build = _state.value.build ?: return
        viewModelScope.launch {
            _state.update { it.copy(confirmDelete = false, isUpdating = true) }
            buildRepository.delete(build.id)
                .onSuccess { _state.update { it.copy(isUpdating = false, isDeleted = true) } }
                .onFailure { error -> _state.update { it.copy(isUpdating = false, message = error.message) } }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
