package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.UserProfile
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * Fila de estadísticas: reseñas, seguidores y siguiendo. Cada una ocupa un
 * tercio del ancho con número y etiqueta centrados, dentro de una tarjeta
 * como el resto de bloques de la app.
 */
@Composable
fun ProfileStatsRow(
    profile: UserProfile,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.sm)
    ) {
        StatItem(
            value = "${profile.reviewsCount}",
            label = stringResource(R.string.user_stat_reviews),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            value = profile.followersDisplay,
            label = stringResource(R.string.user_stat_followers),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onFollowersClick)
        )
        StatItem(
            value = profile.followingDisplay,
            label = stringResource(R.string.user_stat_following),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onFollowingClick)
        )
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
