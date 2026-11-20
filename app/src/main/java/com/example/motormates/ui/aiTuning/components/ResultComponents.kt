package com.example.motormates.ui.aiTuning.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.Build
import com.example.motormates.ui.theme.Radius
import kotlinx.coroutines.delay

private val GENERATING_MESSAGES = listOf(
    "Midiendo los guardabarros…",
    "Montando las piezas…",
    "Ajustando la suspensión…",
    "Puliendo la pintura…",
    "Calibrando las luces del estudio…",
    "Últimos retoques…"
)

/** Espera de 10-20 s mientras Gemini edita la foto: foto original "respirando" + mensajes. */
@Composable
fun GeneratingView(baseImageUrl: String?, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_500)
            messageIndex = (messageIndex + 1) % GENERATING_MESSAGES.size
        }
    }
    val transition = rememberInfiniteTransition(label = "generating")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("generatingView"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(Radius.lg))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (baseImageUrl != null) {
                AsyncImage(
                    model = baseImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(pulse * 0.6f)
                )
            }
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(56.dp)
                    .scale(0.8f + pulse * 0.4f)
            )
        }
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = GENERATING_MESSAGES[messageIndex],
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "message"
        ) { message ->
            Text(message, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "La AI está editando tu foto. Puede tardar unos segundos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCancel) { Text("Cancelar") }
    }
}

/** "Ahora más bajo", "ahora en verde": edición conversacional sobre la imagen actual. */
@Composable
fun RefineBar(
    text: String,
    isRefining: Boolean,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Ajusta con AI", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Ej: más bajo, rines dorados, de noche") },
                enabled = enabled && !isRefining,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("refineField")
            )
            Spacer(Modifier.width(8.dp))
            if (isRefining) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                IconButton(onClick = onSend, enabled = enabled && text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Aplicar ajuste", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Guardar en el perfil o publicar en la comunidad. */
@Composable
fun SaveActions(
    isSaving: Boolean,
    savedBuild: Build?,
    onSave: (publish: Boolean) -> Unit,
    onOpenBuild: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when {
            savedBuild != null -> OutlinedButton(
                onClick = { onOpenBuild(savedBuild.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CloudDone, null)
                Spacer(Modifier.width(8.dp))
                Text(if (savedBuild.published) "Publicado · ver build" else "Guardado · ver build")
            }
            isSaving -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Guardando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onSave(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("saveBuildButton")
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar")
                }
                Button(
                    onClick = { onSave(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("publishBuildButton")
                ) {
                    Icon(Icons.Filled.Public, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Publicar")
                }
            }
        }
    }
}
