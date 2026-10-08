package com.example.motormates.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.model.StoryUser
import com.example.motormates.ui.feed.components.StoriesRow
import com.example.motormates.ui.feed.components.VehicleFeedCard
import com.example.motormates.ui.theme.MotorMatesBackground

@Composable
fun FeedScreenContent(
    stories: List<StoryUser>,
    vehicles: List<FeedVehicleUi>,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onVehicleClick: (Int) -> Unit = {},
    onStoryClick: (Int) -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MotorMatesBackground),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
    ) {
        item { StoriesRow(stories, onStoryClick = onStoryClick) }

        // Lo que viene del backend tiene cuatro estados: cargando, error,
        // vacío o lista con datos.
        when {
            isLoading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onRetryClick,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "Reintentar")
                    }
                }
            }

            vehicles.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todavía no hay vehículos publicados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> items(vehicles, key = { it.id }) { vehicle ->
                VehicleFeedCard(
                    vehicle = vehicle,
                    onClick = { onVehicleClick(vehicle.id) }
                )
            }
        }
    }
}
