package com.example.motormates.ui.followlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.data.model.UserSearchResult

@Composable
fun FollowListScreen(
    uid: String,
    isFollowers: Boolean,
    onBackClick: () -> Unit = {},
    onUserClick: (UserSearchResult) -> Unit = {},
    viewModel: FollowListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uid, isFollowers) {
        viewModel.load(uid, isFollowers)
    }

    FollowListScreenContent(
        users = uiState.users,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        isFollowers = isFollowers,
        onBackClick = onBackClick,
        onUserClick = onUserClick,
        modifier = modifier
    )
}
