package com.example.motormates.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.motormates.data.model.FeedReviewUi
import com.example.motormates.data.model.FeedTab
import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.model.StoryUser
import com.example.motormates.ui.feed.components.FollowingReviewCard
import com.example.motormates.ui.feed.components.StoriesRow
import com.example.motormates.ui.feed.components.VehicleFeedCard
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.Spacing

@Composable
fun FeedScreenContent(
    selectedTab: FeedTab,
    stories: List<StoryUser>,
    vehicles: List<FeedVehicleUi>,
    isLoading: Boolean,
    errorMessage: String?,
    followingReviews: List<FeedReviewUi>,
    isFollowingLoading: Boolean,
    followingErrorMessage: String?,
    modifier: Modifier = Modifier,
    storyErrorMessage: String? = null,
    onVehicleClick: (String) -> Unit = {},
    onStoryClick: (Int) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onDismissStoryError: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onFollowingRetryClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.xxs, bottom = Spacing.gutter)
    ) {
        when (selectedTab) {
            FeedTab.EXPLORAR -> {
                item {
                    StoriesRow(
                        stories,
                        onAddStoryClick = onAddStoryClick,
                        onStoryClick = onStoryClick
                    )
                }

                if (storyErrorMessage != null) {
                    item {
                        Text(
                            text = storyErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.gutter)
                                .clickable(onClick = onDismissStoryError)
                        )
                    }
                }

                // Lo que viene del backend tiene cuatro estados: cargando,
                // error, vacío o lista con datos.
                feedSection(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    isEmpty = vehicles.isEmpty(),
                    emptyMessage = "Todavía no hay vehículos publicados",
                    onRetryClick = onRetryClick
                ) {
                    items(vehicles, key = { it.id }) { vehicle ->
                        VehicleFeedCard(
                            vehicle = vehicle,
                            onClick = { onVehicleClick(vehicle.id) }
                        )
                    }
                }
            }

            FeedTab.SIGUIENDO -> feedSection(
                isLoading = isFollowingLoading,
                errorMessage = followingErrorMessage,
                isEmpty = followingReviews.isEmpty(),
                emptyMessage = "Todavía no sigues a nadie, o quienes sigues no han publicado reseñas",
                onRetryClick = onFollowingRetryClick
            ) {
                items(followingReviews, key = { it.id }) { review ->
                    FollowingReviewCard(
                        review = review,
                        onAuthorClick = onAuthorClick,
                        onVehicleClick = onVehicleClick,
                        onLikeClick = onLikeClick
                    )
                }
            }
        }
    }
}

/**
 * Los cuatro estados de una lista del feed. Se extrae porque las dos
 * pestañas los pintan igual y solo cambia el contenido de la lista.
 */
private fun LazyListScope.feedSection(
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    emptyMessage: String,
    onRetryClick: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    when {
        isLoading -> item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage != null -> item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.padding(top = Spacing.md)
                ) {
                    Text(text = "Reintentar")
                }
            }
        }

        isEmpty -> item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        else -> content()
    }
}
