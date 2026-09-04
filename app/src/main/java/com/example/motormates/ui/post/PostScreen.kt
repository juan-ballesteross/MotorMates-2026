package com.example.motormates.ui.post

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
import com.example.motormates.ui.post.components.PostTopBar
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun PostScreen(
    onCancelClick: () -> Unit = {},
    onPublishClick: (caption: String) -> Unit = {},
    viewModel: PostViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val canPublish = uiState.caption.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PostTopBar(
            canPublish = canPublish,
            onCancelClick = onCancelClick,
            onPublishClick = { onPublishClick(uiState.caption) }
        )
        PostScreenContent(
            caption = uiState.caption,
            onCaptionChange = viewModel::updateCaption,
            taggedVehicle = uiState.taggedVehicle,
            onTagVehicleClick = viewModel::toggleTaggedVehicle,
            location = uiState.location,
            onAddLocationClick = viewModel::toggleLocation
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.FEED) }
        ) { innerPadding ->
            PostScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
