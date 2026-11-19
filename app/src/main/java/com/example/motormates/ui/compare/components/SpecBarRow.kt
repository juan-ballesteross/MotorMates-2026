package com.example.motormates.ui.compare.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.SpecComparisonRow
import com.example.motormates.data.model.SpecWinner
import com.example.motormates.ui.theme.Spacing

/** Color de cada lado: rojo (primary) a la izquierda, dorado (tertiary) a la derecha. */
@Composable
fun leftAccent(): Color = MaterialTheme.colorScheme.primary

@Composable
fun rightAccent(): Color = MaterialTheme.colorScheme.tertiary

/**
 * Una fila del comparador: valor izquierdo, etiqueta y valor derecho, y
 * debajo dos barras que crecen desde el centro hacia afuera. El ganador va
 * en negrita y con su barra en color pleno.
 */
@Composable
fun SpecBarRow(row: SpecComparisonRow, modifier: Modifier = Modifier) {
    val leftWins = row.winner == SpecWinner.LEFT || row.winner == SpecWinner.TIE
    val rightWins = row.winner == SpecWinner.RIGHT || row.winner == SpecWinner.TIE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .testTag("compareRow_${row.label}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecValueText(
                text = row.leftText,
                highlighted = row.winner == SpecWinner.LEFT,
                accent = leftAccent(),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = row.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            SpecValueText(
                text = row.rightText,
                highlighted = row.winner == SpecWinner.RIGHT,
                accent = rightAccent(),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        if (row.showBars) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ComparisonBar(
                    fraction = row.leftFraction,
                    color = leftAccent(),
                    isStrong = leftWins,
                    growsTowardStart = true,
                    modifier = Modifier.weight(1f)
                )
                ComparisonBar(
                    fraction = row.rightFraction,
                    color = rightAccent(),
                    isStrong = rightWins,
                    growsTowardStart = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpecValueText(
    text: String,
    highlighted: Boolean,
    accent: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = if (highlighted) accent else MaterialTheme.colorScheme.onBackground,
        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
        fontSize = 15.sp,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Barra que se llena desde el centro de la fila hacia el borde. */
@Composable
private fun ComparisonBar(
    fraction: Float,
    color: Color,
    isStrong: Boolean,
    growsTowardStart: Boolean,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(targetValue = fraction, label = "compareBar")

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = if (growsTowardStart) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (animated > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(50))
                    .background(if (isStrong) color else color.copy(alpha = 0.4f))
            )
        }
    }
}
