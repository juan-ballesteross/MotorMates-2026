package com.example.motormates.ui.buildDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.motormates.data.model.Build
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.util.CopFormat
import com.example.motormates.ui.aiTuning.components.BeforeAfterSlider
import com.example.motormates.ui.aiTuning.components.InsightsCard
import com.example.motormates.ui.aiTuning.components.SpecsComparisonCard
import com.example.motormates.ui.aiTuning.components.TuningCard

/** Detalle de un Build guardado: antes/después, piezas, costo, rendimiento y ficha. */
@Composable
fun BuildDetailScreen(
    buildId: String,
    onBackClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onDeleted: () -> Unit = {},
    viewModel: BuildDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(buildId) { viewModel.load(buildId) }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onDeleted() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Eliminar build") },
            text = { Text("Se borrará la imagen y la ficha. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    state.build?.vehicleName ?: "Build",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (state.isMine && state.build != null) {
                    IconButton(onClick = viewModel::askDelete) {
                        Icon(Icons.Filled.Delete, "Eliminar build", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        modifier = Modifier.testTag("buildDetailScreen")
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val build = state.build
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                build == null -> Text(
                    state.errorMessage ?: "Este build no existe",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
                else -> BuildDetailContent(
                    build = build,
                    isMine = state.isMine,
                    isUpdating = state.isUpdating,
                    onTogglePublished = viewModel::togglePublished,
                    onAuthorClick = onAuthorClick
                )
            }
        }
    }
}

@Composable
private fun BuildDetailContent(
    build: Build,
    isMine: Boolean,
    isUpdating: Boolean,
    onTogglePublished: () -> Unit,
    onAuthorClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (build.baseImageUrl != null) {
                BeforeAfterSlider(
                    before = {
                        AsyncImage(build.baseImageUrl, "Antes", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    },
                    after = {
                        AsyncImage(build.resultImageUrl, "Después", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                )
            } else {
                AsyncImage(build.resultImageUrl, build.vehicleName, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "por ${build.ownerName ?: "un usuario"}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            if (!isMine) {
                TextButton(onClick = { onAuthorClick(build.ownerId) }) { Text("Ver perfil del autor") }
            }
        }
        if (isMine) {
            item {
                OutlinedButton(onClick = onTogglePublished, enabled = !isUpdating, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (build.published) Icons.Filled.PublicOff else Icons.Filled.Public, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (build.published) "Dejar de publicar" else "Publicar en la comunidad")
                }
            }
        }
        if (build.mode != TuningMode.CATALOG && !build.prompt.isNullOrBlank()) {
            item {
                TuningCard(title = if (build.mode == TuningMode.FREE) "Idea" else "Pieza") {
                    Text(build.prompt, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }
            }
        }
        if (build.partNames.isNotEmpty()) {
            item {
                TuningCard(title = "Piezas") {
                    build.partNames.forEach {
                        Text("• $it", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("Total estimado", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            CopFormat.range(build.summary.totalMinCop, build.summary.totalMaxCop),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item { SpecsComparisonCard(build.summary) }
        }
        build.insights?.let { insights -> item { InsightsCard(insights) } }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
