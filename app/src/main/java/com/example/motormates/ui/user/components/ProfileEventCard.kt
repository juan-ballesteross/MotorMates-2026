package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.EventStatus
import com.example.motormates.data.model.EventType
import com.example.motormates.data.model.MyEvent
import com.example.motormates.data.model.MyEventRole
import com.example.motormates.ui.theme.MotorMatesCarMeetBlue
import com.example.motormates.ui.theme.MotorMatesPiqueGreen
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Un evento de la pestaña "Eventos" del perfil: tipo con su color (el mismo
 * del mapa), fecha, lugar y qué tengo que ver con él (voy, me interesa,
 * organizo). Los cancelados y los pasados se ven atenuados.
 */
@Composable
fun ProfileEventCard(
    item: MyEvent,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = item.event
    val cancelled = event.status == EventStatus.CANCELLED
    val typeColor = if (event.type == EventType.CAR_MEET) MotorMatesCarMeetBlue else MotorMatesPiqueGreen
    val shape = RoundedCornerShape(Radius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = Spacing.xxs)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .alpha(if (isPast || cancelled) 0.6f else 1f)
            .testTag("profileEvent_${event.id}")
    ) {
        // Franja del color del tipo, como en el mapa.
        Box(Modifier.width(6.dp).fillMaxHeight().background(typeColor))
        Column(Modifier.padding(Spacing.sm).weight(1f)) {
            Text(
                text = (if (event.type == EventType.CAR_MEET) "CAR MEET" else "PIQUE") + " · " + formatDate(event.startAt),
                color = typeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = event.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (cancelled) TextDecoration.LineThrough else null
            )
            val place = event.venueName ?: event.location.placeName
            if (!place.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(place, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.size(4.dp))
            if (cancelled) {
                RoleBadge("Cancelado", Icons.Filled.Verified, MaterialTheme.colorScheme.error)
            } else {
                when (item.role) {
                    MyEventRole.GOING -> RoleBadge("Voy", Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary)
                    MyEventRole.INTERESTED -> RoleBadge("Me interesa", Icons.Filled.Star, MaterialTheme.colorScheme.tertiary)
                    MyEventRole.ORGANIZER -> RoleBadge("Organizo", Icons.Filled.Verified, typeColor)
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", Locale.forLanguageTag("es-CO"))

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
