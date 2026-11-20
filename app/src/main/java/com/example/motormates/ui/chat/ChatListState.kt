package com.example.motormates.ui.chat

import com.example.motormates.data.model.ChatPreview
import com.example.motormates.data.model.UserSearchResult

data class ChatListUiState(
    val allChats: List<ChatPreview> = emptyList(),
    val chats: List<ChatPreview> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    // ---- Personas (buscar a alguien aunque no haya chat todavía) ----
    val allUsers: List<UserSearchResult> = emptyList(),
    /** Coinciden con la búsqueda. Vacío mientras no se escriba nada. */
    val people: List<UserSearchResult> = emptyList(),
    val isLoadingPeople: Boolean = false,
    /** A quién sigo (en vivo), con los cambios recién pedidos encima. */
    val followingIds: Set<String> = emptySet()
) {
    val isSearching: Boolean get() = searchQuery.isNotBlank()

    /** Buscando y sin ninguna coincidencia (ni chats ni personas). */
    val hasNoSearchResults: Boolean
        get() = isSearching && chats.isEmpty() && people.isEmpty() && !isLoadingPeople
}
