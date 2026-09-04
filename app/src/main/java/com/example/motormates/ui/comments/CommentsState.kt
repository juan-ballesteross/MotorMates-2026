package com.example.motormates.ui.comments

import com.example.motormates.data.model.CommentUi

data class CommentsUiState(
    val comments: List<CommentUi> = emptyList(),
    val totalCount: Int = 0,
    val draftComment: String = ""
)
