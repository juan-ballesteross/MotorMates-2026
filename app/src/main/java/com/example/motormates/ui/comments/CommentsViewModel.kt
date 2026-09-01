package com.example.motormates.ui.comments

import androidx.lifecycle.ViewModel
import com.example.motormates.data.model.CommentUi
import com.example.motormates.data.model.mockComments
import com.example.motormates.data.model.mockCommentsTotalCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Un MutableStateFlow privado + StateFlow público POR CADA CAMPO
 * (siguiendo el patrón visto en clase), en vez de un único UiState
 * combinado. La vista nunca modifica estos valores directamente —
 * solo puede llamar a las funciones públicas de abajo.
 */
class CommentsViewModel : ViewModel() {

    private val _comments = MutableStateFlow(mockComments)
    val comments: StateFlow<List<CommentUi>> = _comments

    private val _totalCount = MutableStateFlow(mockCommentsTotalCount)
    val totalCount: StateFlow<Int> = _totalCount

    private val _draftComment = MutableStateFlow("")
    val draftComment: StateFlow<String> = _draftComment

    fun updateDraftComment(input: String) {
        _draftComment.value = input
    }

    fun sendButtonPress() {
        val texto = _draftComment.value.trim()
        if (texto.isBlank()) return

        val nuevoComentario = CommentUi(
            id = UUID.randomUUID().toString(),
            authorName = "Tú",
            timeAgo = "ahora",
            text = texto
        )

        _comments.value = _comments.value + nuevoComentario
        _totalCount.value = _totalCount.value + 1
        _draftComment.value = ""
    }

    fun likeButtonPress(commentId: String) {
        _comments.value = _comments.value.map { comentario ->
            if (comentario.id == commentId) {
                comentario.copy(isLiked = !comentario.isLiked)
            } else {
                comentario
            }
        }
    }

    fun replyButtonPress(commentId: String) {
        // TODO: por ahora no hace nada — cuando se defina el comportamiento
        // de "Responder", se implementa aquí, sin tocar la UI.
    }
}