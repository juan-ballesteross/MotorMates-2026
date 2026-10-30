package com.example.motormates.ui.story.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.R

@Composable
fun StoryTopBar(
    avatarUrl: String?,
    userName: String,
    timeAgo: String,
    isOwnStory: Boolean,
    progress: Float,
    onUserClick: () -> Unit,
    onCloseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onUserClick),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
                    .clickable(onClick = onUserClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (timeAgo.isNotBlank()) {
                    Text(
                        text = " · $timeAgo",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            if (isOwnStory) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.story_options_cd),
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { menuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.story_close_cd),
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onCloseClick)
            )
        }
    }
}

@Composable
fun StoryBottomBar(
    caption: String,
    isLiked: Boolean,
    isOwnStory: Boolean,
    likesCount: Long,
    onLikeClick: () -> Unit,
    onLikesCountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = caption, color = Color.White, fontSize = 14.sp)
        }
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.story_like_cd),
                tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .clickable(onClick = onLikeClick)
            )
            if (isOwnStory) {
                Text(
                    text = "$likesCount Me gusta",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onLikesCountClick)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.story_send_cd),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
