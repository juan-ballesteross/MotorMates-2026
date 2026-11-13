package com.example.motormates.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            chatRepository.observeChats().collect { result ->
                result.fold(
                    onSuccess = { chats ->
                        _uiState.update {
                            it.copy(allChats = chats, isLoading = false, errorMessage = null).filterChats()
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "No se pudieron cargar tus mensajes"
                            )
                        }
                    }
                )
            }
        }
    }

    fun updateSearchQuery(input: String) {
        _uiState.update { it.copy(searchQuery = input).filterChats() }
    }

    private fun ChatListUiState.filterChats(): ChatListUiState {
        val filtered = if (searchQuery.isBlank()) {
            allChats
        } else {
            allChats.filter { chat -> chat.otherUserName.contains(searchQuery, ignoreCase = true) }
        }
        return copy(chats = filtered)
    }
}
