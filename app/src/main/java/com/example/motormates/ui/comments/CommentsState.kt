package com.example.motormates.ui.comments

import com.example.motormates.data.model.ReviewUi

data class CommentsUiState(
    val reviews: List<ReviewUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingDeleteReviewId: Int? = null
)
