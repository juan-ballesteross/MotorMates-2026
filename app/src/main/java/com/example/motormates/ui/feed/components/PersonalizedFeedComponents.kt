package com.example.motormates.ui.feed.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.common.components.ShimmerAsyncImage
import com.example.motormates.data.model.FeedVehicleUi
import com.example.motormates.data.util.SuggestedUser
import com.example.motormates.ui.onboarding.components.SuggestedUserRow
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/** Carrusel "Para ti" de Explorar: lo que más coincide con los gustos del onboarding. */
@Composable
fun ForYouCarousel(
    vehicles: List<FeedVehicleUi>,
    onVehicleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("feedForYou")) {
        SectionHeader(title = "Para ti", subtitle = "Según lo que nos contaste")
        Spacer(modifier = Modifier.height(Spacing.xs))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(vehicles, key = { "forYou_${it.id}" }) { vehicle ->
                ForYouCard(vehicle = vehicle, onClick = { onVehicleClick(vehicle.id) })
            }
        }
    }
}

@Composable
private fun ForYouCard(vehicle: FeedVehicleUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .testTag("forYou_${vehicle.id}")
    ) {
        if (vehicle.imageUrl != null) {
            ShimmerAsyncImage(
                model = vehicle.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.sm)
        ) {
            Text(
                text = "${vehicle.brand} ${vehicle.model}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            vehicle.reason?.let { reason ->
                Text(
                    text = reason,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Para quien saltó el onboarding: una invitación a contar sus gustos. */
@Composable
fun PersonalizeFeedCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(Spacing.md)
            .testTag("feedPersonalize"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Personaliza tu feed",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Cuéntanos qué carros te gustan y te mostramos primero lo tuyo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

/** Siguiendo con poco contenido: a quién seguir. */
@Composable
fun FollowSuggestionsSection(
    suggestions: List<SuggestedUser>,
    onFollow: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .testTag("feedFollowSuggestions")
    ) {
        SectionHeader(
            title = "Personas que te pueden interesar",
            subtitle = "Síguelas para ver sus reseñas aquí",
            horizontalPadding = 0.dp
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        suggestions.forEach { user ->
            SuggestedUserRow(
                user = user,
                isFollowed = false,
                onToggleFollow = { onFollow(user.userId) },
                onClick = { onUserClick(user.userId) }
            )
        }
    }
}

@Composable
fun FeedSectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    SectionHeader(title = title, subtitle = subtitle, modifier = modifier)
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Spacing.gutter
) {
    Column(modifier = modifier.padding(horizontal = horizontalPadding)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        if (subtitle != null) {
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

/**
 * Herramientas AI a mano en Explorar: modificar un carro (el propio si hay
 * uno en el garaje) o encontrar una pieza con una foto.
 */
@Composable
fun AiToolsCard(
    myCarName: String?,
    onTuneClick: () -> Unit,
    onFindPartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.md)
            .testTag("feedAiTools")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Herramientas AI", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AiToolButton(
                icon = Icons.Default.Brush,
                title = if (myCarName != null) "Modifica tu $myCarName" else "Modificar con AI",
                subtitle = "Rines, color, aero…",
                onClick = onTuneClick,
                testTag = "feedAiTune",
                modifier = Modifier.weight(1f)
            )
            AiToolButton(
                icon = Icons.Default.ImageSearch,
                title = "Encontrar pieza",
                subtitle = "Con una foto",
                onClick = onFindPartClick,
                testTag = "feedAiFindPart",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AiToolButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(Spacing.sm)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
