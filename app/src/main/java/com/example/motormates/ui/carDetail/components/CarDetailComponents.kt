package com.example.motormates.ui.carDetail.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.carDetail.ReviewUi
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary

/**
 * Foto del carro con los botones de "volver" y "guardar" flotando encima.
 * isBookmarked / onBookmarkClick / onBackClick vienen del padre (state hoisting).
 */
@Composable
fun CarImageHeader(
    imagenResId: Int,
    isBookmarked: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // TODO: reemplazar imagenResId por la foto real del carro (ver CarDetailModels.kt).
        // Mientras tanto (id == 0) se muestra un rectángulo de color para que el
        // Preview no se rompa por intentar cargar un recurso inexistente.
        if (imagenResId != 0) {
            Image(
                painter = painterResource(id = imagenResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(MotorMatesSurface)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", onClick = onBackClick)
            CircleIconButton(
                icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = "Guardar",
                onClick = onBookmarkClick
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

/** Fila de estrellas + calificación numérica + cantidad de reseñas. */
@Composable
fun RatingRow(calificacion: Float, numeroResenas: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MotorMatesRed,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = calificacion.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = " · $numeroResenas reseñas", color = MotorMatesTextSecondary, fontSize = 13.sp)
    }
}

/** Tarjeta individual de especificación (ej. "510 hp" / "Potencia"), usada dentro de un LazyRow. */
@Composable
fun SpecStatCard(valor: String, etiqueta: String) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .background(MotorMatesSurface, shape = RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Text(text = valor, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = etiqueta, color = MotorMatesTextSecondary, fontSize = 11.sp)
    }
}

/** Ítem de reseña de un usuario. Se usa dentro del mismo LazyColumn de la pantalla (no anida otro lazy). */
@Composable
fun ReviewItem(review: ReviewUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (review.avatarResId != 0) {
                    Image(
                        painter = painterResource(id = review.avatarResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MotorMatesSurface)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = review.nombreUsuario, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = review.tiempoTexto, color = MotorMatesTextSecondary, fontSize = 12.sp)
                }
            }

            Row {
                repeat(review.calificacion) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MotorMatesRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = review.comentario, color = MotorMatesTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}