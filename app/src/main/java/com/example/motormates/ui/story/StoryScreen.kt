package com.example.motormates.ui.story

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun StoryScreen(
    storyIndex: Int,
    onCloseClick: () -> Unit = {},
    onUserClick: (Int) -> Unit = {},
    viewModel: StoryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentIndex by remember(storyIndex) { mutableStateOf(storyIndex) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentIndex) {
        viewModel.loadStory(currentIndex)
    }

    val story = uiState.story
    if (story != null) {
        StoryScreenContent(
            story = story,
            isLiked = uiState.isLiked,
            onLikeClick = viewModel::likeButtonPress,
            onCloseClick = onCloseClick,
            onUserClick = { onUserClick(currentIndex) },
            onPreviousClick = {
                val previous = viewModel.previousIndexWithContent(currentIndex - 1)
                if (previous != null) currentIndex = previous
            },
            onNextClick = {
                val next = viewModel.nextIndexWithContent(currentIndex + 1)
                if (next != null) currentIndex = next else onCloseClick()
            },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun StoryScreenPreview() {
    MotorMatesTheme {
        StoryScreen(storyIndex = 0)
    }
}
