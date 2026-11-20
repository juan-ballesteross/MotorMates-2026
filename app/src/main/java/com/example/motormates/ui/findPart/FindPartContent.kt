package com.example.motormates.ui.findPart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.aiTuning.components.GroundedAnswerView
import com.example.motormates.ui.aiTuning.components.TuningImageView
import com.example.motormates.ui.theme.Radius

data class FindPartActions(
    val onBack: () -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onNewSearch: () -> Unit = {},
    val onTuneWithPart: () -> Unit = {},
    val onFinish: () -> Unit = {}
)

/**
 * Tres momentos en la misma pantalla, de arriba abajo: la foto, qué es (opcional)
 * y el resultado de Google. El botón principal cambia según el momento.
 */
@Composable
fun FindPartContent(
    state: FindPartState,
    actions: FindPartActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("findPartScreen"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = actions.onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text("Encontrar pieza", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "Toma o sube una foto de la pieza y la buscamos en Google: qué es, marca y precio.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))

            PhotoBox(state = state, onPick = actions.onPickPhoto, onRemove = actions.onRemovePhoto)

            if (state.image != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = actions.onDescriptionChange,
                    label = { Text("¿Qué es? (opcional)") },
                    placeholder = { Text("Ej. alerón de fibra de carbono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("findPartDescription")
                )
            }

            Spacer(Modifier.height(16.dp))
            val search = state.search
            when {
                search == null -> Button(
                    onClick = actions.onSearch,
                    enabled = state.canSearch,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("findPartSearchButton")
                ) {
                    Icon(Icons.Filled.ImageSearch, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.image == null) "Primero agrega una foto" else "Buscar en Google")
                }
                search.isLoading -> SearchingCard()
                search.error != null -> Column {
                    Text(search.error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = actions.onSearch, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Intentar de nuevo")
                    }
                }
                search.answer != null -> ResultCard(search.answer, actions)
            }
        }
    }
}

@Composable
private fun PhotoBox(state: FindPartState, onPick: () -> Unit, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(Radius.lg)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(enabled = state.image == null && !state.isPreparing, onClick = onPick)
            .testTag("findPartPhoto"),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isPreparing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Quitando el fondo…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            state.image != null -> {
                TuningImageView(state.image, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f), RoundedCornerShape(50))
                ) { Icon(Icons.Filled.Close, contentDescription = "Quitar foto") }
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Tomar o elegir foto", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("Cámara o galería", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SearchingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .testTag("findPartSearching"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Buscando en Google…", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Puedes salir y seguir usando la app: te avisamos con una notificación.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResultCard(answer: com.example.motormates.data.model.GroundedAnswer, actions: FindPartActions) {
    Column(Modifier.testTag("findPartResult")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text("Lo que encontramos", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            GroundedAnswerView(answer)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = actions.onTuneWithPart, modifier = Modifier.fillMaxWidth().testTag("findPartTune")) {
            Icon(Icons.Filled.Brush, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ponerla en un carro con AI")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.onNewSearch, modifier = Modifier.weight(1f).testTag("findPartNew")) {
                Text("Buscar otra pieza")
            }
            TextButton(onClick = actions.onFinish, modifier = Modifier.weight(1f).testTag("findPartFinish")) {
                Icon(Icons.Filled.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Terminar")
            }
        }
    }
}
