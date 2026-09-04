package com.example.motormates.ui.comments

import androidx.lifecycle.ViewModel
import com.example.motormates.data.model.CommentUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class CommentsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState

    fun updateDraftComment(input: String) {
        _uiState.update { it.copy(draftComment = input) }
    }

    fun sendButtonPress() {
        val text = _uiState.value.draftComment.trim()
        if (text.isBlank()) return

        val newComment = CommentUi(
            id = UUID.randomUUID().toString(),
            authorName = "Tú",
            timeAgo = "ahora",
            text = text
        )

        _uiState.update { current ->
            current.copy(
                comments = current.comments + newComment,
                totalCount = current.totalCount + 1,
                draftComment = ""
            )
        }
    }

    fun likeButtonPress(commentId: String) {
        _uiState.update { current ->
            current.copy(
                comments = current.comments.map { comment ->
                    if (comment.id == commentId) {
                        comment.copy(isLiked = !comment.isLiked)
                    } else {
                        comment
                    }
                }
            )
        }
    }

    fun replyButtonPress(commentId: String) {
        // TODO: cuando se defina "Responder", se implementa aquí.
    }
}
