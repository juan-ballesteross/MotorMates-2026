package com.example.motormates.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.R
import com.example.motormates.ui.chat.components.ChatListTopBar
import com.example.motormates.ui.chat.components.ChatPersonRow
import com.example.motormates.ui.chat.components.ChatPreviewRow
import com.example.motormates.ui.theme.Spacing

@Composable
fun ChatListScreen(
    onBackClick: () -> Unit = {},
    onChatClick: (String) -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    /** Tocar a una persona (no el botón de mensaje) abre su perfil. */
    onUserClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ChatListTopBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onBackClick = onBackClick
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null && !uiState.isSearching -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            // Buscando: conversaciones que coinciden y, debajo, personas.
            uiState.isSearching -> SearchResults(
                state = uiState,
                onChatClick = onChatClick,
                onUserClick = onUserClick,
                onToggleFollow = viewModel::toggleFollow
            )

            uiState.chats.isEmpty() -> EmptyMessage(stringResource(R.string.chat_list_empty))

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.chats, key = { it.chatId }) { chat ->
                    ChatPreviewRow(chat = chat, onClick = { onChatClick(chat.otherUserId) })
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: ChatListUiState,
    onChatClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit
) {
    if (state.hasNoSearchResults) {
        EmptyMessage(stringResource(R.string.chat_list_no_search_results))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("chatSearchResults")) {
        if (state.chats.isNotEmpty()) {
            item(key = "chatsHeader") { SectionHeader("Conversaciones") }
            items(state.chats, key = { it.chatId }) { chat ->
                ChatPreviewRow(chat = chat, onClick = { onChatClick(chat.otherUserId) })
            }
        }
        item(key = "peopleHeader") { SectionHeader("Personas") }
        if (state.isLoadingPeople) {
            item(key = "peopleLoading") {
                Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        items(state.people, key = { "person_${it.uid}" }) { person ->
            ChatPersonRow(
                person = person,
                isFollowing = person.uid in state.followingIds,
                onClick = { onUserClick(person.uid) },
                onMessage = { onChatClick(person.uid) },
                onToggleFollow = { onToggleFollow(person.uid) }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.xs)
    )
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
