package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.md3_dark_tertiary

/**
 * Los colores de los marcadores.
 *
 * Los marcadores se rasterizan a bitmap dentro del contenido del mapa, así
 * que no leen MaterialTheme por su cuenta: la pantalla arma esta paleta con
 * el tema activo (fuera del mapa, donde sí está disponible) y se la pasa.
 * Así siguen la paleta clara u oscura sin depender de qué CompositionLocal
 * llegue hasta el mapa.
 */
@Immutable
data class MarkerPalette(
    val border: Color,
    /** Borde del marcador seleccionado: se distingue del de los demás. */
    val selectedBorder: Color,
    /** Fondo del círculo cuando no hay foto. */
    val placeholderBackground: Color,
    val placeholderIcon: Color
) {
    companion object {
        /** La paleta oscura de marca. Es la de los @Preview. */
        val Default = MarkerPalette(
            border = MotorMatesRed,
            selectedBorder = md3_dark_tertiary,
            placeholderBackground = MotorMatesSurface,
            placeholderIcon = MotorMatesTextSecondary
        )

        fun from(colorScheme: ColorScheme) = MarkerPalette(
            border = colorScheme.primary,
            selectedBorder = colorScheme.tertiary,
            placeholderBackground = colorScheme.surface,
            placeholderIcon = colorScheme.onSurfaceVariant
        )
    }
}

private val MarkerSize = 48.dp

/**
 * El marcador de una reseña: la foto del carro en un círculo con borde, la
 * calificación en una etiqueta y una punta abajo que señala el sitio exacto
 * (el ancla del marcador va en la punta, no en el centro del círculo).
 */
@Composable
fun ReviewPhotoMarker(
    photo: ImageBitmap?,
    rating: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    palette: MarkerPalette = MarkerPalette.Default
) {
    val borderColor = if (isSelected) palette.selectedBorder else palette.border
    val size = if (isSelected) MarkerSize + 8.dp else MarkerSize

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            PhotoCircle(photo = photo, size = size, borderColor = borderColor, palette = palette)
            RatingBadge(
                rating = rating,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 2.dp)
            )
        }
        Pointer(color = borderColor)
    }
}

/**
 * Un grupo de reseñas: hasta tres fotos apiladas detrás y, delante, el
 * número de reseñas que agrupa.
 */
@Composable
fun ReviewClusterBubble(
    count: Int,
    photos: List<ImageBitmap?>,
    modifier: Modifier = Modifier,
    palette: MarkerPalette = MarkerPalette.Default
) {
    Box(modifier = modifier.padding(end = 12.dp), contentAlignment = Alignment.CenterStart) {
        photos.take(MAX_STACKED).forEachIndexed { index, photo ->
            // Borde blanco y no del color del tema: separa una foto de la de
            // atrás en los dos mapas (sobre el claro lleva sombra del propio SDK).
            PhotoCircle(
                photo = photo,
                size = 40.dp,
                borderColor = Color.White,
                palette = palette,
                modifier = Modifier.offset(x = (index * 12).dp)
            )
        }
        Box(
            modifier = Modifier
                .offset(x = (photos.take(MAX_STACKED).size.coerceAtLeast(1) * 12).dp, y = 12.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.border)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = if (count > 99) 9.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PhotoCircle(
    photo: ImageBitmap?,
    size: Dp,
    borderColor: Color,
    palette: MarkerPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.placeholderBackground)
            .border(3.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null, // lo describe el título del marcador
                contentScale = ContentScale.Crop,
                // La foto va dentro del borde: si no, lo tapa.
                modifier = Modifier
                    .size(size - 6.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = palette.placeholderIcon,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

@Composable
private fun RatingBadge(rating: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = md3_dark_tertiary,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = rating.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Pointer(color: Color) {
    Canvas(modifier = Modifier.size(width = 12.dp, height = 7.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2, size.height)
            close()
        }
        drawPath(path, color)
        // Se deja un píxel del borde arriba para que la punta se una al círculo sin línea.
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f)
    }
}

private const val MAX_STACKED = 3

@Preview
@Composable
private fun ReviewPhotoMarkerPreview() {
    Row {
        ReviewPhotoMarker(photo = null, rating = 4, isSelected = false)
        ReviewPhotoMarker(photo = null, rating = 5, isSelected = true, modifier = Modifier.padding(start = 16.dp))
    }
}

@Preview
@Composable
private fun ReviewClusterBubblePreview() {
    ReviewClusterBubble(count = 12, photos = listOf(null, null, null))
}

/** La paleta clara de la app, sobre un fondo como el del mapa claro. */
@Preview(backgroundColor = 0xFFF2F2F2, showBackground = true)
@Composable
private fun ReviewPhotoMarkerLightPreview() {
    MotorMatesTheme(darkTheme = true) { // darkTheme = true muestra la paleta clara (ver Theme.kt)
        val palette = MarkerPalette.from(MaterialTheme.colorScheme)
        Row {
            ReviewPhotoMarker(photo = null, rating = 4, isSelected = false, palette = palette)
            ReviewPhotoMarker(
                photo = null, rating = 5, isSelected = true, palette = palette,
                modifier = Modifier.padding(start = 16.dp)
            )
            ReviewClusterBubble(
                count = 7, photos = listOf(null, null), palette = palette,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
