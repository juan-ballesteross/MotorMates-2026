package com.example.motormates.ui.comments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun CommentsScreen(
    onBackClick: () -> Unit = {},
    viewModel: CommentsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CommentsScreenContent(
        comments = uiState.comments,
        totalCount = uiState.totalCount,
        draftComment = uiState.draftComment,
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
