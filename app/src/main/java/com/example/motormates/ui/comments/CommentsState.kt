package com.example.motormates.ui.comments

import com.example.motormates.data.model.CommentUi
import com.example.motormates.data.model.mockComments
import com.example.motormates.data.model.mockCommentsTotalCount

data class CommentsUiState(
    val comments: List<CommentUi> = mockComments,
    val totalCount: Int = mockCommentsTotalCount,
    val draftComment: String = ""
)
