package com.example.motormates.ui.alerts.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.NotificationUi

/** Encabezado de grupo (ej. "HOY", "ESTA SEMANA"). */
@Composable
fun NotificationSectionHeader(titulo: String, modifier: Modifier = Modifier) {
    Text(
        text = titulo,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/** Un ítem de notificación: ícono según tipo + texto (nombre en negrita + mensaje) + hora, y opcionalmente una miniatura. */
@Composable
fun NotificationItem(
    notification: NotificationUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationIcon(type = notification.type)

        Spacer(modifier = Modifier.width(12.dp))

        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            val texto = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(notification.actorName)
                }
                append(" ${notification.message}")
            }
            Text(
                text = texto,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.timeAgo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        if (notification.thumbnailResId != null) {
            Spacer(modifier = Modifier.width(8.dp))
            NotificationThumbnail(resId = notification.thumbnailResId)
        }
    }
}

@Composable
private fun NotificationIcon(type: NotificationType) {
    val (icon, tint) = when (type) {
        NotificationType.LIKE -> Icons.Filled.Favorite to MaterialTheme.colorScheme.primary
        NotificationType.RATING -> Icons.Filled.Star to MaterialTheme.colorScheme.primary
        NotificationType.COMMENT -> Icons.AutoMirrored.Filled.Comment to MaterialTheme.colorScheme.onSurfaceVariant
        NotificationType.FOLLOW -> Icons.Filled.Person to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Miniatura de la reseña/publicación referenciada. Si no hay imagen todavía, muestra el placeholder "foto" del diseño. */
@Composable
private fun NotificationThumbnail(resId: Int) {
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "foto", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}