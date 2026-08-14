package com.example.motormates.ui.common.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary

enum class MainBottomDestination { FEED, EXPLORE }

@Composable
fun MainBottomNavBar(
    selected: MainBottomDestination,
    modifier: Modifier = Modifier,
    onFeedClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onPublishClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MotorMatesSurface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Filled.Home,
            label = stringResource(R.string.common_bottom_nav_feed),
            selected = selected == MainBottomDestination.FEED,
            onClick = onFeedClick
        )
        BottomNavItem(
            icon = Icons.Filled.Explore,
            label = stringResource(R.string.common_bottom_nav_explore),
            selected = selected == MainBottomDestination.EXPLORE,
            onClick = onExploreClick
        )
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MotorMatesRed, CircleShape)
                .clickable(onClick = onPublishClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.common_bottom_nav_publish_cd),
                tint = Color.White
            )
        }
        BottomNavItem(
            icon = Icons.Filled.Notifications,
            label = stringResource(R.string.common_bottom_nav_alerts),
            selected = false,
            onClick = onAlertsClick
        )
        BottomNavItem(
            icon = Icons.Filled.Person,
            label = stringResource(R.string.common_bottom_nav_profile),
            selected = false,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val color = if (selected) MotorMatesRed else MotorMatesTextSecondary
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = color, fontSize = 11.sp)
    }
}
