package com.example.motormates.ui.publicProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.ui.publicProfile.components.PublicProfileHeader
import com.example.motormates.ui.user.components.ProfileCarCard
import com.example.motormates.ui.user.components.ProfileEmptyTabState
import com.example.motormates.ui.user.components.ProfileInfoSection
import com.example.motormates.ui.user.components.ProfileStatsRow
import com.example.motormates.ui.user.components.ProfileTabsRow

@Composable
fun PublicProfileScreenContent(
    coverResId: Int,
    avatarResId: Int,
    profile: UserProfile,
    cars: List<GarageCar>,
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    onBackClick: () -> Unit,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PublicProfileHeader(
                coverResId = coverResId,
                avatarResId = avatarResId,
                onBackClick = onBackClick,
                onFollowClick = onFollowClick,
                onMessageClick = onMessageClick
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileInfoSection(profile = profile)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileStatsRow(profile = profile)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileTabsRow(selected = selectedTab, onSelect = onSelectTab)
        }
        when (selectedTab) {
            ProfileTab.ACTIVITY -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
                }
            }
            ProfileTab.REVIEWS, ProfileTab.GARAGE -> {
                gridItems(cars) { car ->
                    ProfileCarCard(car)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
