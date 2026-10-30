package com.example.motormates.ui.story

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.followlist.components.FollowListUserRow
import com.example.motormates.ui.theme.MotorMatesTheme
import kotlinx.coroutines.delay

private const val STORY_DURATION_MS = 5000L
private const val STORY_STEP_MS = 50L

@Composable
fun StoryScreen(
    storyIndex: Int,
    onCloseClick: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    viewModel: StoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var currentIndex by remember(storyIndex) { mutableStateOf(storyIndex) }
    var progress by remember(currentIndex) { mutableStateOf(0f) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentIndex) {
        viewModel.loadStory(currentIndex)
    }

    val story = uiState.story

    LaunchedEffect(currentIndex, story?.id, uiState.pendingDelete, uiState.showLikers) {
        if (story == null || uiState.pendingDelete || uiState.showLikers) return@LaunchedEffect
        progress = 0f
        val steps = (STORY_DURATION_MS / STORY_STEP_MS).toInt()
        repeat(steps) { step ->
            delay(STORY_STEP_MS)
            progress = (step + 1) / steps.toFloat()
        }
        val next = viewModel.nextIndexWithContent(currentIndex + 1)
        if (next != null) currentIndex = next else onCloseClick()
    }

    if (uiState.pendingDelete) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteStory,
            title = { Text(text = "Eliminar historia") },
            text = { Text(text = "¿Seguro que quieres eliminar tu historia? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteStory(onDeleted = onCloseClick) }) {
                    Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteStory) {
                    Text(text = "Cancelar")
                }
            }
        )
    }

    if (uiState.showLikers) {
        Dialog(onDismissRequest = viewModel::dismissLikers) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Me gusta (${uiState.likers.size})",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    when {
                        uiState.isLoadingLikers -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        uiState.likers.isEmpty() -> Text(
                            text = "Nadie le ha dado like todavía",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        else -> LazyColumn {
                            items(uiState.likers, key = { it.uid }) { liker ->
                                FollowListUserRow(
                                    user = liker,
                                    onClick = {
                                        viewModel.dismissLikers()
                                        onUserClick(liker.uid)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (story != null) {
            StoryScreenContent(
                story = story,
                isLiked = uiState.isLiked,
                isOwnStory = uiState.isOwnStory,
                likesCount = uiState.likesCount,
                progress = progress,
                onLikeClick = viewModel::likeButtonPress,
                onLikesCountClick = viewModel::askShowLikers,
                onCloseClick = onCloseClick,
                onUserClick = { onUserClick(story.userId) },
                onDeleteClick = viewModel::askDeleteStory,
                onPreviousClick = {
                    val previous = viewModel.previousIndexWithContent(currentIndex - 1)
                    if (previous != null) currentIndex = previous
                },
                onNextClick = {
                    val next = viewModel.nextIndexWithContent(currentIndex + 1)
                    if (next != null) currentIndex = next else onCloseClick()
                }
            )
        }
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(onClick = viewModel::dismissError)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StoryScreenPreview() {
    MotorMatesTheme {
        StoryScreen(storyIndex = 0)
    }
}
