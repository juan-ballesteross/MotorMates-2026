package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.ActivityType
import com.example.motormates.data.model.ActivityUi
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/** Título de grupo de la línea de tiempo: "HOY", "ESTA SEMANA", "ANTES". */
@Composable
fun ActivitySectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier.padding(top = Spacing.xs, start = 4.dp)
    )
}

/**
 * Una fila de la actividad: ícono del tipo, el texto con los nombres en
 * negrita, cuándo fue y, si hay, la miniatura del carro o de la historia.
 */
@Composable
fun ActivityRow(item: ActivityUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, tint) = activityIcon(item.type)
    val clickable = item.vehicleId != null || item.userId != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = clickable, onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            .testTag("activityRow_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(Spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    item.parts.forEach { part ->
                        if (part.bold) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
                        } else {
                            append(part.text)
                        }
                    }
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.rating?.let { rating ->
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = item.timeAgo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        item.imageUrl?.let { url ->
            Spacer(modifier = Modifier.width(Spacing.sm))
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 56.dp, height = 44.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun activityIcon(type: ActivityType): Pair<ImageVector, Color> = when (type) {
    ActivityType.REVIEW -> Icons.Filled.RateReview to MaterialTheme.colorScheme.primary
    ActivityType.VEHICLE -> Icons.Filled.DirectionsCar to MaterialTheme.colorScheme.tertiary
    ActivityType.LIKE -> Icons.Filled.Favorite to MaterialTheme.colorScheme.secondary
    ActivityType.FOLLOW -> Icons.Filled.PersonAdd to MaterialTheme.colorScheme.onSurfaceVariant
    ActivityType.STORY -> Icons.Filled.AutoStories to MaterialTheme.colorScheme.tertiary
}
