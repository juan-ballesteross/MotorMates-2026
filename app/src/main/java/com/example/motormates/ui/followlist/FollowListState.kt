package com.example.motormates.ui.followlist

import com.example.motormates.data.model.UserSearchResult

data class FollowListUiState(
    val users: List<UserSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
