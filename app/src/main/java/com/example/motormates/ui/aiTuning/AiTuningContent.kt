package com.example.motormates.ui.aiTuning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.util.TuningPromptBuilder
import com.example.motormates.ui.aiTuning.components.BeforeAfterSlider
import com.example.motormates.ui.aiTuning.components.CategoryChipsRow
import com.example.motormates.ui.aiTuning.components.CostBreakdownCard
import com.example.motormates.ui.aiTuning.components.FreeIdeaPanel
import com.example.motormates.ui.aiTuning.components.GeneratingView
import com.example.motormates.ui.aiTuning.components.GroundedStatus
import com.example.motormates.ui.aiTuning.components.InsightsCard
import com.example.motormates.ui.aiTuning.components.OwnPartPanel
import com.example.motormates.ui.aiTuning.components.PackagesRow
import com.example.motormates.ui.aiTuning.components.PartCard
import com.example.motormates.ui.aiTuning.components.RefineBar
import com.example.motormates.ui.aiTuning.components.SaveActions
import com.example.motormates.ui.aiTuning.components.SelectionBar
import com.example.motormates.ui.aiTuning.components.SpecsComparisonCard
import com.example.motormates.ui.aiTuning.components.TuningCard
import com.example.motormates.ui.aiTuning.components.TuningImageView
import com.example.motormates.ui.aiTuning.components.TuningModeSelector
import com.example.motormates.ui.aiTuning.components.VehiclePickerContent
import com.example.motormates.ui.theme.Radius

/** Callbacks del configurador, agrupados para no pasar veinte lambdas sueltas. */
data class AiTuningActions(
    val onBack: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onSelectMode: (TuningMode) -> Unit = {},
    val onSelectCategory: (PartCategory) -> Unit = {},
    val onTogglePart: (String) -> Unit = {},
    val onApplyPackage: (String) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onFreeIdeaChange: (String) -> Unit = {},
    val onOwnPartDescriptionChange: (String) -> Unit = {},
    val onPickOwnPart: () -> Unit = {},
    val onRemoveOwnPart: () -> Unit = {},
    val onIdentifyOwnPart: () -> Unit = {},
    val onGenerate: () -> Unit = {},
    val onCancelGeneration: () -> Unit = {},
    val onRefineTextChange: (String) -> Unit = {},
    val onRefine: () -> Unit = {},
    val onRetryInsights: () -> Unit = {},
    val onLivePrice: (String) -> Unit = {},
    val onSave: (Boolean) -> Unit = {},
    val onOpenBuild: (String) -> Unit = {},
    val onBackToConfigurator: () -> Unit = {},
    // --- elegir carro y terminar ---
    val onPickerQueryChange: (String) -> Unit = {},
    val onPickerCategory: (String) -> Unit = {},
    val onPickVehicle: (String) -> Unit = {},
    val onRetryPicker: () -> Unit = {},
    val onChangeVehicle: () -> Unit = {},
    val onFinish: () -> Unit = {}
)

/** Todo el estado llega del padre; aquí solo se pinta. */
@Composable
fun AiTuningContent(
    state: AiTuningState,
    actions: AiTuningActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("aiTuningScreen"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TuningTopBar(
                title = if (state.phase == TuningPhase.PICK_VEHICLE) "Elige un carro"
                else state.vehicle?.let { TuningPromptBuilder.vehicleName(it) } ?: "AI Tuning",
                // Atrás retrocede un paso: resultado → piezas → (si se eligió) carros → salir.
                onBack = when {
                    state.phase == TuningPhase.RESULT -> actions.onBackToConfigurator
                    state.phase == TuningPhase.CONFIGURING && state.canChangeVehicle -> actions.onChangeVehicle
                    else -> actions.onBack
                }
            )
        },
        bottomBar = {
            if (state.phase == TuningPhase.CONFIGURING && state.vehicle != null && state.loadError == null) {
                SelectionBar(
                    mode = state.mode,
                    summary = state.summary,
                    canGenerate = state.canGenerate,
                    remainingGenerations = state.remainingGenerations,
                    onClear = actions.onClearSelection,
                    onGenerate = actions.onGenerate
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.phase == TuningPhase.PICK_VEHICLE -> VehiclePickerContent(
                    myVehicles = state.myPickerVehicles,
                    otherVehicles = state.otherPickerVehicles,
                    categories = state.pickerCategories,
                    query = state.pickerQuery,
                    selectedCategory = state.pickerCategory,
                    isLoading = state.isPickerLoading,
                    error = state.pickerError,
                    onQueryChange = actions.onPickerQueryChange,
                    onSelectCategory = actions.onPickerCategory,
                    onPick = actions.onPickVehicle,
                    onRetry = actions.onRetryPicker
                )
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.loadError != null -> LoadError(state.loadError, actions.onRetryLoad, actions.onBack)
                state.phase == TuningPhase.GENERATING -> GeneratingView(
                    baseImageUrl = state.vehicle?.imageUrl,
                    onCancel = actions.onCancelGeneration,
                    onLeave = actions.onBack
                )
                state.phase == TuningPhase.RESULT -> ResultContent(state, actions)
                else -> ConfiguratorContent(state, actions)
            }
        }
    }
}

@Composable
private fun TuningTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("AI TUNING", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LoadError(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Volver") }
            OutlinedButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun ConfiguratorContent(state: AiTuningState, actions: AiTuningActions) {
    val vehicle = state.vehicle ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                AsyncImage(
                    model = vehicle.imageUrl,
                    contentDescription = TuningPromptBuilder.vehicleName(vehicle),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(MaterialTheme.colorScheme.surface)
                )
                // Se entró sin carro: se puede cambiar sin salir de la pantalla.
                if (state.canChangeVehicle) {
                    FilledTonalButton(
                        onClick = actions.onChangeVehicle,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .testTag("changeVehicleButton")
                    ) {
                        Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cambiar carro")
                    }
                }
            }
            state.busyWithVehicleName?.let { name ->
                Text(
                    "Ya se está modificando tu $name. Cuando termine podrás generar esta.",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            TuningModeSelector(
                selected = state.mode,
                onSelect = actions.onSelectMode,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        when (state.mode) {
            TuningMode.CATALOG -> {
                if (state.packages.isNotEmpty()) {
                    item {
                        SectionTitle("Paquetes rápidos")
                        PackagesRow(state.packages, state.activePackageId, actions.onApplyPackage)
                        Spacer(Modifier.height(16.dp))
                    }
                }
                item {
                    SectionTitle("Piezas")
                    CategoryChipsRow(
                        categories = state.categories,
                        selected = state.selectedCategory,
                        selection = state.selection,
                        onSelect = actions.onSelectCategory
                    )
                    Spacer(Modifier.height(12.dp))
                }
                // Cuadrícula de 2 columnas dentro del mismo LazyColumn (no se anida otro lazy).
                items(state.partsInSelectedCategory.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { part ->
                            PartCard(
                                part = part,
                                isSelected = state.selection[part.category] == part.id,
                                onClick = { actions.onTogglePart(part.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            TuningMode.OWN_PART -> item {
                OwnPartPanel(
                    image = state.ownPartImage,
                    description = state.ownPartDescription,
                    isPreparing = state.isPreparingOwnPart,
                    identification = state.ownPartIdentification,
                    onPickPhoto = actions.onPickOwnPart,
                    onRemovePhoto = actions.onRemoveOwnPart,
                    onDescriptionChange = actions.onOwnPartDescriptionChange,
                    onIdentify = actions.onIdentifyOwnPart,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            TuningMode.FREE -> item {
                FreeIdeaPanel(
                    idea = state.freeIdea,
                    onIdeaChange = actions.onFreeIdeaChange,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultContent(state: AiTuningState, actions: AiTuningActions) {
    val vehicle = state.vehicle ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BeforeAfterSlider(
                before = {
                    AsyncImage(
                        model = vehicle.imageUrl,
                        contentDescription = "Antes",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                after = { TuningImageView(state.resultImage) }
            )
            if (state.resultMode == TuningMode.FREE) {
                Text(
                    "Diseño libre: sin precio ni especificaciones verificados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        item {
            RefineBar(
                text = state.refineText,
                isRefining = state.isRefining,
                enabled = state.remainingGenerations != 0,
                onTextChange = actions.onRefineTextChange,
                onSend = actions.onRefine
            )
        }
        if (state.resultMode == TuningMode.CATALOG && state.resultParts.isNotEmpty()) {
            item { SpecsComparisonCard(state.resultSummary) }
            item {
                CostBreakdownCard(
                    parts = state.resultParts,
                    summary = state.resultSummary,
                    trailing = { part ->
                        val live = state.livePrices[part.id]
                        if (live == null) {
                            Text(
                                "Ver precio actual en Google",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { actions.onLivePrice(part.id) }
                            )
                        } else {
                            GroundedStatus(live)
                        }
                    }
                )
            }
            item {
                when {
                    state.insights != null -> InsightsCard(state.insights)
                    state.isInsightsLoading -> TuningCard(title = "Ficha técnica AI") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Analizando el build…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    state.insightsError != null -> TuningCard(title = "Ficha técnica AI") {
                        Text(state.insightsError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        TextButton(onClick = actions.onRetryInsights) { Text("Reintentar") }
                    }
                }
            }
        }
        item {
            SaveActions(
                isSaving = state.isSaving,
                savedBuild = state.savedBuild,
                onSave = actions.onSave,
                onOpenBuild = actions.onOpenBuild
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = actions.onBackToConfigurator, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Tune, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cambiar piezas")
            }
            Spacer(Modifier.height(8.dp))
            // Salir del todo, sin tener que darle "atrás" varias veces.
            TextButton(
                onClick = actions.onFinish,
                modifier = Modifier.fillMaxWidth().testTag("finishTuningButton")
            ) {
                Icon(Icons.Filled.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Terminar e ir al inicio")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    )
}

