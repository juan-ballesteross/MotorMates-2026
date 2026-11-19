package com.example.motormates.ui.feed.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.ui.common.components.ShimmerAsyncImage
import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.seed.localAvatarResFor
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.example.motormates.ui.theme.pressScaleClickable

@Composable
fun VehicleFeedCard(
    vehicle: FeedVehicleUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .shadow(elevation = Elevation.sm, shape = RoundedCornerShape(Radius.lg))
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.lg))
            .pressScaleClickable(onClick = onClick)
    ) {
        if (vehicle.ownerId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuthorClick(vehicle.ownerId) }
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VehicleOwnerAvatar(ownerId = vehicle.ownerId, avatarUrl = vehicle.ownerAvatarUrl)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = vehicle.ownerName ?: "Usuario",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        if (vehicle.imageUrl != null) {
            ShimmerAsyncImage(
                model = vehicle.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        } else {
            // Mientras el backend no tenga imageUrl, se muestra este placeholder.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "${vehicle.brand} ${vehicle.model}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "${vehicle.year} · ${vehicle.category}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun VehicleOwnerAvatar(ownerId: String, avatarUrl: String?) {
    val avatarModifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
    val localAvatarRes = localAvatarResFor(ownerId)
    when {
        avatarUrl != null -> AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
        localAvatarRes != null -> Image(
            painter = painterResource(localAvatarRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
        else -> Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
