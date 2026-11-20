package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.Build
import com.example.motormates.data.util.CopFormat
import com.example.motormates.ui.theme.Radius

/**
 * Miniatura de un build de AI Tuning en la cuadrícula del perfil.
 * showVisibility: en mi perfil se marca si es público o privado.
 */
@Composable
fun ProfileBuildCard(
    build: Build,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    showVisibility: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick(build.id) }
            .testTag("profileBuild_${build.id}")
    ) {
        Box {
            AsyncImage(
                model = build.resultImageUrl,
                contentDescription = build.vehicleName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
            if (showVisibility) {
                Icon(
                    imageVector = if (build.published) Icons.Filled.Public else Icons.Filled.Lock,
                    contentDescription = if (build.published) "Público" else "Privado",
                    // Siempre sobre la foto: fondo negro semitransparente fijo.
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(3.dp)
                        .size(14.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = build.vehicleName,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (build.partNames.isEmpty()) "Diseño AI"
                else "${build.partNames.size} piezas · ${CopFormat.compact(build.summary.totalMaxCop)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
