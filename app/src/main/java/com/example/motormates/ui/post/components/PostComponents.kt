package com.example.motormates.ui.post.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R

/**
 * Vista previa de la galería del dispositivo. Todavía no hay un selector de
 * fotos real integrado (ver PostModels.kt / TODOs en PostScreen.kt), así que
 * se usa esta imagen fija (res/drawable/grid_gallery.png) que simula la
 * cuadrícula del carrete, atenuada al 50% con Modifier.alpha para que se vea
 * claramente que es un placeholder y no fotos reales seleccionables.
 */
@Composable
fun PostGalleryPreview(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.grid_gallery),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2000f / 1414f)
            .alpha(0.5f)
    )
}

/**
 * Campo para la descripción de la publicación. Mismos colores/forma que el
 * OutlinedTextField de SearchTopBar, pero multilínea (minLines) porque aquí
 * se espera un texto más largo que una búsqueda de una sola línea.
 */
@Composable
fun PostCaptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.post_caption_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        minLines = 4,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Botón de acción secundaria (etiquetar vehículo / agregar ubicación).
 * selectedValue es null cuando todavía no se ha elegido nada; cuando tiene
 * un valor, el botón se resalta en rojo y muestra ese valor junto al label.
 */
@Composable
fun PostActionButton(
    icon: ImageVector,
    label: String,
    selectedValue: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedValue != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isSelected) "$label: $selectedValue" else label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
