package com.example.motormates.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.motormates.data.model.ReviewPost
import com.example.motormates.data.model.StoryUser
import com.example.motormates.ui.feed.components.ReviewPostCard
import com.example.motormates.ui.feed.components.StoriesRow
import com.example.motormates.ui.theme.MotorMatesBackground

@Composable
fun FeedScreenContent(
    stories: List<StoryUser>,
    post: ReviewPost,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        StoriesRow(stories)
        Spacer(modifier = Modifier.height(20.dp))
        ReviewPostCard(post)
        Spacer(modifier = Modifier.height(20.dp))
    }
}
