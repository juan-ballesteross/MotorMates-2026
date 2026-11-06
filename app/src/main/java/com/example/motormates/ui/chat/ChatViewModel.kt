package com.example.motormates.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.ChatRepository
import com.example.motormates.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var messagesJob: Job? = null

    fun load(otherUid: String) {
        if (_uiState.value.otherUserId == otherUid && _uiState.value.chatId != null) return

        _uiState.update { it.copy(otherUserId = otherUid, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            userRepository.getCurrentUserProfile(otherUid).onSuccess { user ->
                _uiState.update { it.copy(otherUserName = user.fullName, otherUserAvatarUrl = user.avatarUrl) }
            }

            chatRepository.openChatWith(otherUid).fold(
                onSuccess = { chatId ->
                    _uiState.update { it.copy(chatId = chatId, isLoading = false) }
                    chatRepository.markChatAsRead(chatId)
                    observeMessages(chatId)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    private fun observeMessages(chatId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
                chatRepository.markChatAsRead(chatId)
            }
        }
    }

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val chatId = state.chatId ?: return
        val text = state.draft
        if (text.isBlank() || state.isSending) return

        _uiState.update { it.copy(draft = "", isSending = true) }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(chatId, state.otherUserId, text)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSending = false) } },
                onFailure = { error ->
                    _uiState.update { it.copy(isSending = false, draft = text, errorMessage = error.message) }
                }
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
