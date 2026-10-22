package com.example.motormates.ui.publicProfile.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.user.components.AVATAR_OVERLAP
import com.example.motormates.ui.user.components.COVER_HEIGHT

@Composable
fun PublicProfileHeader(
    coverResId: Int,
    coverUrl: String? = null,
    avatarResId: Int,
    avatarUrl: String? = null,
    isFollowing: Boolean = false,
    onBackClick: () -> Unit,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mismo esquema que ProfileHeader: Box en vez de offset para no dejar
    // un hueco bajo el avatar.
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = stringResource(R.string.user_cover_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(COVER_HEIGHT)
                        .clip(RoundedCornerShape(Radius.xl))
                )
            } else {
                Image(
                    painter = painterResource(coverResId),
                    contentDescription = stringResource(R.string.user_cover_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(COVER_HEIGHT)
                        .clip(RoundedCornerShape(Radius.xl))
                )
            }
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.public_profile_back_cd),
                    tint = Color.White
                )
            }
        }
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
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = stringResource(R.string.user_avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(13.dp))
                    )
                } else {
                    Image(
                        painter = painterResource(avatarResId),
                        contentDescription = stringResource(R.string.user_avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(13.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isFollowing) {
                OutlinedButton(
                    onClick = onFollowClick,
                    shape = RoundedCornerShape(Radius.sm)
                ) {
                    Text(
                        text = stringResource(R.string.public_profile_following_button),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(Radius.sm)
                ) {
                    Text(
                        text = stringResource(R.string.public_profile_follow_button),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = onMessageClick,
                shape = RoundedCornerShape(Radius.sm)
            ) {
                Text(
                    text = stringResource(R.string.public_profile_message_button),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
