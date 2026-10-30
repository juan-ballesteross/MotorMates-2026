package com.example.motormates.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.StoryUser

@Composable
fun StoriesRow(
    stories: List<StoryUser>,
    modifier: Modifier = Modifier,
    onAddStoryClick: () -> Unit = {},
    onStoryClick: (Int) -> Unit = {}
) {
    val coverIndices = remember(stories) {
        val seenUserIds = mutableSetOf<String>()
        stories.indices.filter { index -> seenUserIds.add(stories[index].userId) }
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { AddStoryItem(onClick = onAddStoryClick) }
        items(coverIndices, key = { index -> stories[index].userId }) { index ->
            val story = stories[index]
            StoryItem(story, onClick = { if (story.imageUrl != null) onStoryClick(index) })
        }
    }
}

@Composable
private fun AddStoryItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Tu historia", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
    }
}

@Composable
private fun StoryItem(story: StoryUser, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (story.avatarUrl != null) {
                AsyncImage(
                    model = story.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = story.name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = story.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
    }
}
