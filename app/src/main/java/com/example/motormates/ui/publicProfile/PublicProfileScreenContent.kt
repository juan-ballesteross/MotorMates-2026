package com.example.motormates.ui.publicProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.motormates.data.model.Build
import com.example.motormates.data.model.GarageVehicleUi
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.data.model.UserReviewUi
import com.example.motormates.ui.common.components.GarageEmptyState
import com.example.motormates.ui.common.components.GarageVehicleCard
import com.example.motormates.ui.publicProfile.components.PublicProfileHeader
import com.example.motormates.ui.user.components.ProfileBuildCard
import com.example.motormates.ui.user.components.ProfileEmptyTabState
import com.example.motormates.ui.user.components.ProfileInfoSection
import com.example.motormates.ui.user.components.ProfileReviewCard
import com.example.motormates.ui.user.components.ProfileStatsRow
import com.example.motormates.ui.user.components.ProfileTabsRow
import com.example.motormates.ui.theme.Spacing

@Composable
fun PublicProfileScreenContent(
    coverResId: Int,
    coverUrl: String? = null,
    avatarResId: Int,
    avatarUrl: String? = null,
    profile: UserProfile,
    reviews: List<UserReviewUi>,
    isLoading: Boolean,
    errorMessage: String?,
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    onBackClick: () -> Unit,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onReviewClick: (String) -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    garage: List<GarageVehicleUi> = emptyList(),
    onGarageCarClick: (String) -> Unit = {},
    onCompareWithMineClick: (String) -> Unit = {},
    builds: List<Build> = emptyList(),
    onBuildClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("publicProfileScreen"),
        contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PublicProfileHeader(
                coverResId = coverResId,
                coverUrl = coverUrl,
                avatarResId = avatarResId,
                avatarUrl = avatarUrl,
                isFollowing = isFollowing,
                onBackClick = onBackClick,
                onFollowClick = onFollowClick,
                onMessageClick = onMessageClick
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileInfoSection(profile = profile)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileStatsRow(
                profile = profile,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileTabsRow(selected = selectedTab, onSelect = onSelectTab)
        }
        when (selectedTab) {
            // El backend no modela la actividad de otros usuarios.
            ProfileTab.ACTIVITY -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
                }
            }
            // Los carros a su nombre, cada uno con "Comparar con el mío".
            ProfileTab.GARAGE -> if (garage.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GarageEmptyState(message = "Todavía no tiene carros en su garaje")
                }
            } else {
                gridItems(items = garage, key = { "garage_${it.id}" }) { car ->
                    GarageVehicleCard(
                        car = car,
                        onClick = { onGarageCarClick(car.id) },
                        onCompareWithMineClick = { onCompareWithMineClick(car.id) }
                    )
                }
            }
            // Solo los builds que este usuario decidió publicar.
            ProfileTab.BUILDS -> if (builds.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
                }
            } else {
                gridItems(builds, key = { it.id }) { build ->
                    ProfileBuildCard(build = build, onClick = onBuildClick)
                }
            }
            // Las reseñas que ESTE usuario ha realizado.
            ProfileTab.REVIEWS -> when {
                isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = Spacing.lg)
                    )
                }

                reviews.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
                }

                else -> gridItems(
                    items = reviews,
                    key = { it.id },
                    span = { GridItemSpan(maxLineSpan) }
                ) { review ->
                    ProfileReviewCard(
                        review = review,
                        onClick = onReviewClick,
                        onCompareWithMineClick = onCompareWithMineClick
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
