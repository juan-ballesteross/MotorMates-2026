package com.example.motormates.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.model.toUserSearchResult
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.ChatRepository
import com.example.motormates.data.repository.FollowRepository
import com.example.motormates.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lista de conversaciones. El buscador encuentra chats existentes y, además,
 * personas con las que todavía no hay chat: se les puede escribir o seguir
 * desde aquí mismo.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState

    /** Seguir/dejar de seguir recién tocado, hasta que lo confirme el listener. */
    private val pendingFollow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private var usersLoaded = false

    init {
        load()
        observeFollowing()
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

    private fun observeFollowing() {
        val uid = authRepository.getCurrentUser()?.id ?: return
        viewModelScope.launch {
            combine(followRepository.observeFollowingIds(uid), pendingFollow) { server, pending ->
                // Se suelta lo pendiente que el servidor ya confirmó.
                val settled = pending.filter { (id, follow) -> (id in server) == follow }.keys
                if (settled.isNotEmpty()) pendingFollow.update { it - settled }
                pending.entries.fold(server.toSet()) { ids, (id, follow) -> if (follow) ids + id else ids - id }
            }.collect { ids -> _uiState.update { it.copy(followingIds = ids) } }
        }
    }

    fun updateSearchQuery(input: String) {
        _uiState.update { it.copy(searchQuery = input).filterChats().filterPeople() }
        // Los usuarios se cargan la primera vez que se busca, no al abrir la lista.
        if (input.isNotBlank() && !usersLoaded) loadUsers()
    }

    private fun loadUsers() {
        usersLoaded = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPeople = true) }
            val currentUid = authRepository.getCurrentUser()?.id
            userRepository.getAllRegisteredUsers()
                .onSuccess { users ->
                    _uiState.update {
                        it.copy(
                            allUsers = users.filter { user -> user.id != currentUid }.map { user -> user.toUserSearchResult() },
                            isLoadingPeople = false
                        ).filterPeople()
                    }
                }
                .onFailure {
                    usersLoaded = false
                    _uiState.update { it.copy(isLoadingPeople = false) }
                }
        }
    }

    /** Optimista: el botón cambia ya; si la escritura falla, vuelve a como estaba. */
    fun toggleFollow(targetUid: String) {
        val uid = authRepository.getCurrentUser()?.id ?: return
        val following = targetUid in _uiState.value.followingIds
        pendingFollow.update { it + (targetUid to !following) }
        viewModelScope.launch {
            val result = if (following) {
                followRepository.unfollowUser(uid, targetUid)
            } else {
                followRepository.followUser(uid, targetUid)
            }
            result.onFailure { pendingFollow.update { it - targetUid } }
        }
    }

    private fun ChatListUiState.filterChats(): ChatListUiState {
        val filtered = if (searchQuery.isBlank()) {
            allChats
        } else {
            allChats.filter { chat -> chat.otherUserName.contains(searchQuery, ignoreCase = true) }
        }
        return copy(chats = filtered)
    }

    private fun ChatListUiState.filterPeople(): ChatListUiState {
        val query = searchQuery.trim().removePrefix("@")
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            allUsers.filter { user ->
                user.name.contains(query, ignoreCase = true) || user.handle.contains(query, ignoreCase = true)
            }.take(MAX_PEOPLE)
        }
        return copy(people = results)
    }

    private companion object {
        const val MAX_PEOPLE = 20
    }
}
