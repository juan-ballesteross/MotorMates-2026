package com.example.motormates.ui.story

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.motormates.data.model.StoryUser
import com.example.motormates.ui.story.components.StoryBottomBar
import com.example.motormates.ui.story.components.StoryTopBar

@Composable
fun StoryScreenContent(
    story: StoryUser,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCloseClick: () -> Unit,
    onUserClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRes = story.imageRes ?: return
    val caption = story.caption.orEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = caption,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPreviousClick
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNextClick
                    )
            )
        }
        StoryTopBar(
            avatarResId = story.avatarResId,
            userName = story.name,
            onUserClick = onUserClick,
            onCloseClick = onCloseClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        StoryBottomBar(
            caption = caption,
            isLiked = isLiked,
            onLikeClick = onLikeClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
