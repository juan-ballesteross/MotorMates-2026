package com.example.motormates.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.GarageVehicleUi
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Un carro del garaje, en la grilla de dos columnas del perfil. En el perfil
 * de otro usuario lleva "Comparar con el mío"; en el propio no (onCompareWithMineClick = null).
 */
@Composable
fun GarageVehicleCard(
    car: GarageVehicleUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCompareWithMineClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .testTag("garageCar_${car.id}")
    ) {
        val imageModifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
        if (car.imageUrl != null) {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.title,
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
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
            Text(
                text = car.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = car.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onCompareWithMineClick != null) {
            CompareWithMineButton(
                onClick = onCompareWithMineClick,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .testTag("compareWithMine_${car.id}")
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * El garaje vacío. message cambia según de quién es el perfil; en el propio
 * se pasa onPublishClick para invitar a publicar el primer carro.
 */
@Composable
fun GarageEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    onPublishClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Garage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (onPublishClick != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = onPublishClick,
                shape = RoundedCornerShape(Radius.md),
                modifier = Modifier.testTag("garagePublishButton")
            ) {
                Text(text = "Publicar mi vehículo")
            }
        }
    }
}
