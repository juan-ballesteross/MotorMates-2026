package com.example.motormates.ui.feed

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.feed.components.FeedTopBar
import com.example.motormates.ui.feed.model.sampleStories
import com.example.motormates.ui.feed.model.samplePost
import com.example.motormates.ui.theme.MotorMatesBackground

@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    onExploreClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        containerColor = MotorMatesBackground,
        topBar = { FeedTopBar() },
        bottomBar = {
            MainBottomNavBar(
                selected = MainBottomDestination.FEED,
                onExploreClick = onExploreClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        FeedScreenContent(
            stories = sampleStories,
            post = samplePost,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
