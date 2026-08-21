package com.example.motormates.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.ui.common.components.MainBottomDestination
import com.example.motormates.ui.common.components.MainBottomNavBar
import com.example.motormates.ui.feed.components.FeedTopBar
import com.example.motormates.ui.feed.model.sampleStories
import com.example.motormates.ui.feed.model.samplePost
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Ya NO tiene su propio Scaffold — el único Scaffold de la app vive en
 * MainActivity.kt (MotorMatesApp). Esta función solo devuelve el topBar
 * de Feed + su contenido, para que el padre la coloque dentro del
 * Scaffold central junto con el bottomBar compartido.
 */
@Composable
fun FeedScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
    ) {
        FeedTopBar()
        FeedScreenContent(
            stories = sampleStories,
            post = samplePost
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    MotorMatesTheme {
        Scaffold(
            containerColor = MotorMatesBackground,
            bottomBar = { MainBottomNavBar(selected = MainBottomDestination.FEED) }
        ) { innerPadding ->
            FeedScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
