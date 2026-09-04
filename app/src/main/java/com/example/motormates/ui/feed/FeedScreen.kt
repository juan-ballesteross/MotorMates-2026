package com.example.motormates.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.feed.components.FeedTopBar
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FeedTopBar()
        FeedScreenContent(
            stories = uiState.stories,
            post = uiState.post
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.FEED) }
        ) { innerPadding ->
            FeedScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
