package com.example.motormates.ui.feed.components

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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.FeedReviewUi
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Una reseña de alguien que sigo. Mezcla lo de ReviewItem (autor y like) con
 * lo de ProfileReviewCard (el carro del que habla), porque en el feed las
 * dos cosas son nuevas para quien lee.
 */
@Composable
fun FollowingReviewCard(
    review: FeedReviewUi,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.sm)
    ) {
        Row(
            modifier = Modifier.clickable { onAuthorClick(review.userId) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthorAvatar(avatarUrl = review.avatarUrl, name = review.nombreUsuario)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = review.nombreUsuario,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = review.tiempoTexto,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.clickable { onVehicleClick(review.vehicleId) },
            verticalAlignment = Alignment.Top
        ) {
            VehicleThumbnail(imageUrl = review.vehicleImageUrl)
            Spacer(modifier = Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.vehicleName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(review.calificacion) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (review.comentario.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = review.comentario,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLikeClick(review.id) }
        ) {
            Text(
                text = "${review.likesCount}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(Spacing.xxs))
            Icon(
                imageVector = if (review.isLikedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (review.isLikedByMe) "Quitar like" else "Dar like",
                tint = if (review.isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Sin foto, la inicial del nombre, igual que en la lista de seguidores. */
@Composable
private fun AuthorAvatar(avatarUrl: String?, name: String) {
    val avatarModifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    } else {
        Box(modifier = avatarModifier, contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Miniatura del vehículo; sin foto, el mismo placeholder del perfil. */
@Composable
private fun VehicleThumbnail(imageUrl: String?) {
    val thumbnailModifier = Modifier
        .size(72.dp)
        .clip(RoundedCornerShape(Radius.md))
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = thumbnailModifier
        )
    } else {
        Box(modifier = thumbnailModifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
