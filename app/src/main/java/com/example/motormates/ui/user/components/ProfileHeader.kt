package com.example.motormates.ui.user.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.testTag
import coil.compose.SubcomposeAsyncImage
import com.example.motormates.ui.common.components.NeutralCover
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import coil.compose.AsyncImage
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.pressScaleClickable

/**
 * Portada + avatar superpuesto + rueda de configuración (abre Editar
 * perfil), con las fotos reales del usuario. Sin portada subida se ve un
 * degradado neutro, no una foto de ejemplo.
 */
@Composable
fun ProfileHeader(
    profileImageUrl: String?,
    coverImageUrl: String?,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Box y no Column + offset: offset solo mueve el dibujo, el espacio
    // original se sigue midiendo y dejaba un hueco bajo el avatar. Así el
    // avatar se superpone a la portada y la altura total es la real.
    Box(modifier = modifier.fillMaxWidth()) {
        val coverModifier = Modifier
            .fillMaxWidth()
            .height(COVER_HEIGHT)
            .clip(RoundedCornerShape(Radius.xl))
        if (coverImageUrl != null) {
            SubcomposeAsyncImage(
                model = coverImageUrl,
                contentDescription = stringResource(R.string.user_cover_cd),
                contentScale = ContentScale.Crop,
                loading = { NeutralCover(Modifier.fillMaxSize()) },
                error = { NeutralCover(Modifier.fillMaxSize()) },
                modifier = coverModifier
            )
        } else {
            NeutralCover(coverModifier)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(COVER_HEIGHT)
                .clip(RoundedCornerShape(Radius.xl))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = COVER_HEIGHT - AVATAR_OVERLAP),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(elevation = Elevation.md, shape = RoundedCornerShape(Radius.lg))
                    .clip(RoundedCornerShape(Radius.lg))
                    .border(3.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(Radius.lg))
            ) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = stringResource(R.string.user_avatar_cd),
                    placeholder = painterResource(R.drawable.user_3),
                    error = painterResource(R.drawable.user_3),
                    fallback = painterResource(R.drawable.user_3),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(13.dp))
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // La configuración de la cuenta (perfil, garaje, gustos, cerrar sesión).
            // Sin marco: solo el ícono, con el toque circular de IconButton.
            IconButton(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("profileSettingsButton")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.user_edit_profile_button),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/** Compartidas con PublicProfileHeader para que ambos perfiles midan igual. */
internal val COVER_HEIGHT = 140.dp
internal val AVATAR_OVERLAP = 38.dp
