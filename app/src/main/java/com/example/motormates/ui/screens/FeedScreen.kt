package com.example.motormates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesRedLight
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary
import com.example.motormates.ui.theme.MotorMatesTheme

data class StoryUser(val name: String)

data class ReviewPost(
    val userName: String,
    val carName: String,
    val timeAgo: String,
    val rating: Int,
    val caption: String,
    val likes: Int,
    val comments: Int,
    val shares: Int
)

private val sampleStories = listOf("Marco", "Elena", "Diego", "Sofía").map { StoryUser(it) }

private val samplePost = ReviewPost(
    userName = "Marco Ferretti",
    carName = "Porsche 911 GT3",
    timeAgo = "2h",
    rating = 5,
    caption = "Primera vuelta con el GT3 en Ascari. La dirección es de otro planeta.",
    likes = 284,
    comments = 32,
    shares = 9
)

@Composable
fun FeedScreen(onExploreClick: () -> Unit = {}) {
    Scaffold(
        containerColor = MotorMatesBackground,
        topBar = { FeedTopBar() },
        bottomBar = { FeedBottomBar(onExploreClick = onExploreClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MotorMatesBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            StoriesRow(sampleStories)
            Spacer(modifier = Modifier.height(20.dp))
            ReviewPostCard(samplePost)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeedTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MotorMatesBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MotorMates",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color.White)
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Filled.Notifications, contentDescription = "Alertas", tint = Color.White)
    }
}

@Composable
private fun StoriesRow(stories: List<StoryUser>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(stories) { story ->
            StoryItem(story)
        }
    }
}

@Composable
private fun StoryItem(story: StoryUser) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.linearGradient(listOf(MotorMatesRedLight, MotorMatesRed)),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = story.name, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun ReviewPostCard(post: ReviewPost) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(MotorMatesRedLight, MotorMatesRed)),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = post.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row {
                    Text(text = "reseñó el ", color = MotorMatesTextSecondary, fontSize = 13.sp)
                    Text(text = post.carName, color = MotorMatesRedLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = " · ${post.timeAgo}", color = MotorMatesTextSecondary, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(color = MotorMatesSurface, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MotorMatesTextSecondary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < post.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = MotorMatesRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = post.caption, color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PostActionStat(icon = Icons.Filled.FavoriteBorder, count = post.likes)
            Spacer(modifier = Modifier.width(20.dp))
            PostActionStat(icon = Icons.Filled.ChatBubbleOutline, count = post.comments)
            Spacer(modifier = Modifier.width(20.dp))
            PostActionStat(icon = Icons.Filled.Share, count = post.shares)
        }
    }
}

@Composable
private fun PostActionStat(icon: ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MotorMatesTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$count", color = MotorMatesTextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun FeedBottomBar(onExploreClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MotorMatesSurface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(icon = Icons.Filled.Home, label = "Feed", selected = true)
        BottomNavItem(icon = Icons.Filled.Explore, label = "Explorar", selected = false, onClick = onExploreClick)
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color = MotorMatesRed, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Publicar", tint = Color.White)
        }
        BottomNavItem(icon = Icons.Filled.Notifications, label = "Alertas", selected = false)
        BottomNavItem(icon = Icons.Filled.Person, label = "Perfil", selected = false)
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit = {}) {
    val color = if (selected) MotorMatesRed else MotorMatesTextSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = color, fontSize = 11.sp)
    }
}