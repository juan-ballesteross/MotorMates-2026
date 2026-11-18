package com.example.motormates.ui.compare.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.CompareCarUi
import com.example.motormates.data.model.SpecWinner
import com.example.motormates.data.model.VehicleComparison
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

@Composable
fun CompareTopBar(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Comparar",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
        )
    }
}

@Composable
fun CompareSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = modifier.padding(top = Spacing.lg, bottom = Spacing.xxs)
    )
}

/** Resumen arriba de la tabla: quién gana más categorías. */
@Composable
fun VerdictCard(comparison: VehicleComparison, modifier: Modifier = Modifier) {
    val winner = comparison.overallWinner
    val (title, accent) = when (winner) {
        SpecWinner.LEFT -> "Gana el ${comparison.left.displayName()}" to leftAccent()
        SpecWinner.RIGHT -> "Gana el ${comparison.right.displayName()}" to rightAccent()
        SpecWinner.TIE -> "Empate técnico" to MaterialTheme.colorScheme.onSurface
        SpecWinner.NONE -> "No hay datos suficientes" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.md)
            .testTag("compareVerdict"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = null, tint = accent)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${comparison.leftWins} – ${comparison.rightWins} en las categorías comparadas",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

/** La reseña con más likes de cada carro, una debajo de la otra. */
@Composable
fun TopReviewsSection(left: CompareCarUi, right: CompareCarUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        TopReviewCard(car = left, accent = leftAccent())
        TopReviewCard(car = right, accent = rightAccent())
    }
}

@Composable
private fun TopReviewCard(car: CompareCarUi, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.sm)
    ) {
        Text(
            text = car.displayName(),
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))

        val review = car.topReview
        if (review == null) {
            Text(
                text = "Todavía no tiene reseñas con comentario.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (index < review.calificacion) accent else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = "“${review.comentario}”",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "— ${review.autor}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = review.likesCount.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

private fun CompareCarUi.displayName(): String =
    "${marca.lowercase().replaceFirstChar { it.uppercase() }} $modelo"
