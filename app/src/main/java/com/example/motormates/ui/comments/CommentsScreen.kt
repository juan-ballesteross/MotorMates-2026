package com.example.motormates.ui.comments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Observa cada StateFlow del ViewModel por separado (comments, totalCount,
 * draftComment) — no hay un único "uiState" combinado, igual que se vio
 * en clase. Las funciones del ViewModel se llaman directamente desde los
 * callbacks que espera el Content.
 */
@Composable
fun CommentsScreen(
    onBackClick: () -> Unit = {},
    viewModel: CommentsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val draftComment by viewModel.draftComment.collectAsStateWithLifecycle()

    CommentsScreenContent(
        comments = comments,
        totalCount = totalCount,
        draftComment = draftComment,
        onBackClick = onBackClick,
        onDraftChange = viewModel::updateDraftComment,
        onSendClick = viewModel::sendButtonPress,
        onLikeClick = viewModel::likeButtonPress,
        onReplyClick = viewModel::replyButtonPress,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun CommentsScreenPreview() {
    MotorMatesTheme {
        CommentsScreen()
    }
}