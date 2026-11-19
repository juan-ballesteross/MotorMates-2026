package com.example.motormates.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.ui.common.components.ShimmerAsyncImage
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.example.motormates.ui.theme.pressScaleClickable
import com.example.motormates.ui.theme.rememberPressScale

@Composable
fun ChatTopBar(
    name: String,
    avatarUrl: String?,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.chat_back_cd),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.md))
                .pressScaleClickable(onClick = onProfileClick)
                .padding(vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
    }
}

@Composable
fun MessageBubble(
    text: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    storyImageUrl: String? = null,
    isStoryReply: Boolean = false
) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            // Respuesta a una historia: arriba se ve cuál, como en otras apps.
            if (isStoryReply) {
                Text(
                    text = if (isMine) "Respondiste a su historia" else "Respondió a tu historia",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (storyImageUrl != null) {
                    AsyncImage(
                        model = storyImageUrl,
                        contentDescription = "Historia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(width = 96.dp, height = 150.dp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            if (imageUrl != null) {
                ShimmerAsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.chat_image_cd),
                    modifier = Modifier
                        .padding(bottom = if (text.isBlank()) 0.dp else 4.dp)
                        .widthIn(max = 220.dp)
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            if (text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .shadow(elevation = Elevation.xs, shape = RoundedCornerShape(Radius.md))
                        .background(bubbleColor, RoundedCornerShape(Radius.md))
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                ) {
                    Text(text = text, color = textColor, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    pickedImageUri: Uri? = null,
    onAttachImageClick: () -> Unit = {},
    onRemovePickedImageClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        if (pickedImageUri != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                    .size(64.dp)
            ) {
                AsyncImage(
                    model = pickedImageUri,
                    contentDescription = stringResource(R.string.chat_image_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                IconButton(
                    onClick = onRemovePickedImageClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.chat_remove_image_cd),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachImageClick) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = stringResource(R.string.chat_attach_image_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                modifier = Modifier.weight(1f)
            )
            val sendInteractionSource = remember { MutableInteractionSource() }
            val sendScale = rememberPressScale(sendInteractionSource)
            IconButton(
                onClick = onSendClick,
                interactionSource = sendInteractionSource,
                modifier = Modifier.scale(sendScale)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send_cd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
