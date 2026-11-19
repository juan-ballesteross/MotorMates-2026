package com.example.motormates.ui.compare.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.CompareCarUi
import com.example.motormates.data.model.CompareSide
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.example.motormates.ui.theme.pressScaleClickable

/**
 * Las dos tarjetas enfrentadas con un "VS" en el medio. La izquierda es el
 * carro base (desde el que se entró): no se cambia ni se intercambia, todo
 * se compara contra él. La derecha se cambia con "Cambiar"; si todavía no
 * hay carro, se muestra un hueco para elegirlo.
 */
@Composable
fun CompareHeader(
    left: CompareCarUi?,
    right: CompareCarUi?,
    onCarClick: (String) -> Unit,
    onChangeClick: (CompareSide) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (left != null) {
            CompareCarCard(
                car = left,
                accent = leftAccent(),
                onClick = { onCarClick(left.id) },
                // Sin "Cambiar": es el carro base de la comparación.
                onChangeClick = null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("compareLeftCar")
            )
        } else {
            EmptyCarSlot(onClick = { onChangeClick(CompareSide.LEFT) }, modifier = Modifier.weight(1f))
        }

        // Antes era un botón de intercambiar; con un carro base fijo a la
        // izquierda ya no aplica.
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (right != null) {
            CompareCarCard(
                car = right,
                accent = rightAccent(),
                onClick = { onCarClick(right.id) },
                onChangeClick = { onChangeClick(CompareSide.RIGHT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("compareRightCar")
            )
        } else {
            EmptyCarSlot(
                onClick = { onChangeClick(CompareSide.RIGHT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("compareEmptySlot")
            )
        }
    }
}

@Composable
private fun CompareCarCard(
    car: CompareCarUi,
    accent: Color,
    onClick: () -> Unit,
    /** null = carro base: en lugar del botón va la etiqueta "Carro base". */
    onChangeClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(elevation = Elevation.sm, shape = RoundedCornerShape(Radius.md))
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.md))
            .pressScaleClickable(onClick = onClick)
    ) {
        CarImage(imageUrl = car.imageUrl)
        Column(modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)) {
            Text(
                text = car.marca,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = car.modelo,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${car.anio} · ${car.categoria}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onChangeClick != null) {
            TextButton(
                onClick = onChangeClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("compareChangeRight")
            ) {
                Text(text = "Cambiar", color = accent, fontSize = 13.sp)
            }
        } else {
            // Mismo alto que el botón de la otra tarjeta, para que queden parejas.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Carro base",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CarImage(imageUrl: String?) {
    val imageModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 10f)
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    } else {
        Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Hueco punteado para elegir el segundo carro. */
@Composable
private fun EmptyCarSlot(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(Radius.md))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(Radius.md)
            )
            .pressScaleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = rightAccent()
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Elegir carro",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

