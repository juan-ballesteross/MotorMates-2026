package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.EventType
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * El marcador de un evento: un círculo con el ícono del tipo y una punta
 * abajo, para que el ancla caiga justo en el lugar del evento.
 *
 * No lleva foto, al contrario que [ReviewPhotoMarker]: lo que importa de un
 * evento al mirar el mapa es qué tipo es y cuándo es, no qué carro aparece en
 * la imagen. Eso evita además precargar bitmaps para esta capa.
 *
 * La paleta llega por parámetro y no se lee de MaterialTheme aquí dentro: el
 * contenido de Clustering se rasteriza fuera de la composición.
 */
@Composable
fun EventMarker(
    type: EventType,
    isSelected: Boolean,
    isCancelled: Boolean,
    modifier: Modifier = Modifier,
    palette: MarkerPalette = MarkerPalette.Default
) {
    val size = if (isSelected) SELECTED_SIZE else NORMAL_SIZE
    val color = when {
        // Un evento cancelado se apaga en vez de desaparecer: quien dijo que
        // iba necesita verlo para enterarse.
        isCancelled -> palette.placeholderIcon
        type == EventType.CAR_MEET -> palette.carMeetBorder
        else -> palette.piqueBorder
    }
    val borderColor = if (isSelected) palette.selectedBorder else color

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .border(3.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null, // lo describe el título del marcador
                tint = Color.White,
                modifier = Modifier.size(size / 2)
            )
        }
        EventPointer(borderColor)
    }
}

/**
 * La burbuja de un grupo de eventos. Muestra el conteo y, si el grupo es de
 * un solo tipo, su ícono; si mezcla Car Meets y piques, no muestra ninguno,
 * porque enseñar solo uno de los dos sería engañoso.
 */
@Composable
fun EventClusterBubble(
    count: Int,
    types: Set<EventType>,
    modifier: Modifier = Modifier,
    palette: MarkerPalette = MarkerPalette.Default
) {
    val singleType = types.singleOrNull()
    val color = when (singleType) {
        EventType.CAR_MEET -> palette.carMeetBorder
        EventType.PIQUE -> palette.piqueBorder
        null -> palette.placeholderBackground
    }

    Box(
        modifier = modifier
            .size(CLUSTER_SIZE)
            .clip(CircleShape)
            .background(color)
            .border(3.dp, palette.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (singleType != null) {
            Icon(
                imageVector = singleType.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(CLUSTER_SIZE / 2)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            Text(
                // Igual que la burbuja de reseñas: por encima de 99 el número
                // no cabe sin encoger la fuente.
                text = if (count > MAX_COUNT) "$MAX_COUNT+" else count.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

private val EventType.icon
    get() = when (this) {
        EventType.CAR_MEET -> Icons.Filled.Groups
        EventType.PIQUE -> Icons.Filled.EmojiEvents
    }

/** Misma punta que el marcador de reseña, para que las capas se vean hermanas. */
@Composable
private fun EventPointer(color: Color) {
    Canvas(modifier = Modifier.size(width = 12.dp, height = 7.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2, size.height)
            close()
        }
        drawPath(path, color)
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f)
    }
}

private val NORMAL_SIZE: Dp = 40.dp
private val SELECTED_SIZE: Dp = 48.dp
private val CLUSTER_SIZE: Dp = 44.dp
private const val MAX_COUNT = 99

@Preview
@Composable
private fun EventMarkerPreview() {
    MotorMatesTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            EventMarker(type = EventType.CAR_MEET, isSelected = false, isCancelled = false)
        }
    }
}

@Preview
@Composable
private fun EventMarkerPiqueSelectedPreview() {
    MotorMatesTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            EventMarker(type = EventType.PIQUE, isSelected = true, isCancelled = false)
        }
    }
}

@Preview
@Composable
private fun EventClusterBubblePreview() {
    MotorMatesTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            EventClusterBubble(count = 7, types = setOf(EventType.CAR_MEET))
        }
    }
}
