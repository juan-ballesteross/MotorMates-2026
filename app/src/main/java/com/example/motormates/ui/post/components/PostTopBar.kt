package com.example.motormates.ui.post.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesTextSecondary

/**
 * Barra superior de "Nueva publicación": "Cancelar" a la izquierda, título
 * centrado y "Publicar" a la derecha. "Publicar" se deshabilita (se ve
 * atenuado y deja de responder al toque) mientras canPublish sea false,
 * que en PostScreen depende de que haya al menos una foto seleccionada.
 */
@Composable
fun PostTopBar(
    canPublish: Boolean,
    onCancelClick: () -> Unit,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MotorMatesBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = stringResource(R.string.post_cancel_button),
            color = MotorMatesTextSecondary,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onCancelClick)
        )
        Text(
            text = stringResource(R.string.post_top_bar_title),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = stringResource(R.string.post_publish_button),
            color = if (canPublish) MotorMatesRed else MotorMatesRed.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(enabled = canPublish, onClick = onPublishClick)
        )
    }
}
