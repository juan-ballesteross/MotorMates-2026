package com.example.motormates.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag

/**
 * Portada de un perfil que todavía no ha subido la suya: un degradado
 * neutro con los colores del tema, en vez de una foto de ejemplo que
 * parecería del usuario.
 */
@Composable
fun NeutralCover(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(colors.surfaceVariant, colors.surface, colors.surfaceVariant.copy(alpha = 0.7f)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .testTag("neutralCover")
    )
}
