package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import coil.compose.AsyncImage
import com.example.motormates.ui.theme.Radius

/**
 * Portada + avatar superpuesto + botón "Editar perfil", replicando el
 * diseño de Figma de la pantalla de usuario con las fotos reales del
 * usuario (portada, avatar).
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
        AsyncImage(
            model = coverImageUrl,
            contentDescription = stringResource(R.string.user_cover_cd),
            placeholder = painterResource(R.drawable.profile_cover),
            error = painterResource(R.drawable.profile_cover),
            fallback = painterResource(R.drawable.profile_cover),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(COVER_HEIGHT)
                .clip(RoundedCornerShape(Radius.xl))
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
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(Radius.sm))
                    .clickable(onClick = onEditProfileClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.user_edit_profile_button),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Compartidas con PublicProfileHeader para que ambos perfiles midan igual. */
internal val COVER_HEIGHT = 140.dp
internal val AVATAR_OVERLAP = 38.dp
