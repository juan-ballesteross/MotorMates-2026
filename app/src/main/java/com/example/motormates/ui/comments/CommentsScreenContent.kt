package com.example.motormates.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.motormates.data.model.CommentUi
import com.example.motormates.ui.comments.components.CommentInputBar
import com.example.motormates.ui.comments.components.CommentItem
import com.example.motormates.ui.comments.components.CommentsTopBar

/**
 * Stateless: recibe cada campo por separado (comments, totalCount,
 * draftComment) tal como los expone el ViewModel — no arma nada,
 * no sabe que existe CommentsViewModel.
 */
@Composable
fun CommentsScreenContent(
    comments: List<CommentUi>,
    totalCount: Int,
    draftComment: String,
    onBackClick: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CommentsTopBar(totalCount = totalCount, onBackClick = onBackClick)
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    onLikeClick = { onLikeClick(comment.id) },
                    onReplyClick = { onReplyClick(comment.id) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            }
        }

        CommentInputBar(
            value = draftComment,
            onValueChange = onDraftChange,
            onSendClick = onSendClick
        )
    }
}