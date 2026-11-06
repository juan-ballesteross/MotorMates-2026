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
class ChatBadgeViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _hasUnreadChats = MutableStateFlow(false)
    val hasUnreadChats: StateFlow<Boolean> = _hasUnreadChats

    init {
        viewModelScope.launch {
            chatRepository.observeHasUnreadChats().collect { hasUnread ->
                _hasUnreadChats.update { hasUnread }
            }
        }
    }
}
