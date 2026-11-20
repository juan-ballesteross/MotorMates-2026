package com.example.motormates.ui.aiTuning.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningMode
import com.example.motormates.data.model.TuningPackage
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.util.CopFormat
import com.example.motormates.ui.aiTuning.GroundedUi
import com.example.motormates.ui.theme.Radius

/** Catálogo / Trae tu pieza / Describe tu idea. */
@Composable
fun TuningModeSelector(selected: TuningMode, onSelect: (TuningMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = listOf(
        TuningMode.CATALOG to "Catálogo",
        TuningMode.OWN_PART to "Tu pieza",
        TuningMode.FREE to "Tu idea"
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                modifier = Modifier.testTag("tuningMode_${mode.name}")
            ) { Text(label, fontSize = 13.sp) }
        }
    }
}

/** Paquetes de un toque. */
@Composable
fun PackagesRow(
    packages: List<TuningPackage>,
    activePackageId: String?,
    onPackageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (packages.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(packages, key = { it.id }) { pkg ->
            val isActive = pkg.id == activePackageId
            Column(
                modifier = Modifier
                    .width(170.dp)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent),
                        RoundedCornerShape(Radius.md)
                    )
                    .clickable { onPackageClick(pkg.id) }
                    .padding(12.dp)
                    .testTag("package_${pkg.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pkg.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    pkg.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoryChipsRow(
    categories: List<PartCategory>,
    selected: PartCategory,
    selection: Map<PartCategory, String>,
    onSelect: (PartCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(categories, key = { it.name }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.label) },
                leadingIcon = {
                    Icon(
                        imageVector = if (category in selection) Icons.Filled.CheckCircle else category.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                modifier = Modifier.testTag("category_${category.name}")
            )
        }
    }
}

/** Tarjeta de pieza: foto (o ícono si no tiene), nombre, rango de precio e insignias. */
@Composable
fun PartCard(part: Part, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                RoundedCornerShape(Radius.md)
            )
            .clickable(onClick = onClick)
            .testTag("part_${part.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            when {
                part.colorHex != null -> ColorSwatch(part.colorHex, part.finish)
                part.imageUrl != null -> AsyncImage(
                    model = part.imageUrl,
                    contentDescription = part.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Icon(
                    imageVector = part.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.White, CircleShape)
                )
            }
            if (!part.visible) {
                Badge(
                    text = "No visible",
                    icon = Icons.Filled.VisibilityOff,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                part.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                part.brand,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                CopFormat.range(part.priceMinCop, part.priceMaxCop),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            val badges = buildList {
                if (part.hpDeltaPct != 0.0) add("${signed(part.hpDeltaPct)} % HP")
                if (part.weightDeltaKg != 0.0) add("${signed(part.weightDeltaKg)} kg")
                if (part.heightDeltaMm != 0) add("${signed(part.heightDeltaMm.toDouble())} mm")
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    badges.joinToString(" · "),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, finish: String?) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )
        if (finish != null) {
            Badge(text = finish, modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp))
        }
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = Color.White, fontSize = 10.sp)
    }
}

/** Panel de "Trae tu pieza": foto de la pieza, descripción e identificación. */
@Composable
fun OwnPartPanel(
    image: TuningImage?,
    description: String,
    isPreparing: Boolean,
    identification: GroundedUi?,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIdentify: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Toma o elige una foto de una pieza real (un rin, un alerón, una foto en otra pantalla…). " +
                "La app le quita el fondo en el teléfono y la AI la instala en tu carro.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Radius.md))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = !isPreparing && image == null, onClick = onPickPhoto)
                .testTag("ownPartPhoto"),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = rememberTuningBitmap(image)
            when {
                isPreparing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Quitando el fondo…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                bitmap != null -> {
                    Image(bitmap = bitmap, contentDescription = "Tu pieza", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Quitar foto",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable(onClick = onRemovePhoto)
                            .padding(4.dp)
                    )
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Tomar o elegir foto", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("¿Qué pieza es? (opcional)") },
            placeholder = { Text("Ej: rin de 18 pulgadas color bronce") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )
        if (image != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onIdentify,
                enabled = identification?.isLoading != true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Identificar pieza y precio")
            }
            GroundedStatus(identification)
        }
    }
}

/** Panel de "Describe tu idea". */
@Composable
fun FreeIdeaPanel(idea: String, onIdeaChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = idea,
            onValueChange = onIdeaChange,
            label = { Text("Describe tu idea") },
            placeholder = { Text("Ej: franjas de carreras rojas estilo vintage y rines dorados") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("freeIdeaField"),
            minLines = 3,
            maxLines = 5
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Diseño libre: solo se genera la imagen, sin precio ni cambios de especificaciones verificados.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

/** Resultado de una búsqueda con Google: cargando, error o respuesta con fuentes. */
@Composable
fun GroundedStatus(ui: GroundedUi?, modifier: Modifier = Modifier) {
    when {
        ui == null -> Unit
        ui.isLoading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Buscando en Google…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        ui.error != null -> Text(ui.error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = modifier.padding(top = 8.dp))
        ui.answer != null -> GroundedAnswerView(ui.answer, modifier = modifier.padding(top = 8.dp))
    }
}

/**
 * Barra inferior fija: cuántas piezas, total y cambio de potencia, en vivo
 * (calculado sin AI), y el botón Generar.
 */
@Composable
fun SelectionBar(
    mode: TuningMode,
    summary: TuningSummary,
    canGenerate: Boolean,
    remainingGenerations: Int?,
    onClear: () -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (mode == TuningMode.CATALOG) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (summary.partsCount) {
                            0 -> "Elige piezas o un paquete"
                            1 -> "1 pieza"
                            else -> "${summary.partsCount} piezas"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (summary.partsCount > 0) {
                        Text(
                            CopFormat.range(summary.totalMinCop, summary.totalMaxCop),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.testTag("selectionTotal")
                        )
                    }
                }
                val hpDelta = summary.hpDelta
                if (hpDelta != null && hpDelta != 0.0) {
                    Text(
                        "${signed(hpDelta)} HP",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                if (summary.partsCount > 0) {
                    Text(
                        "Limpiar",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onClear)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = onGenerate,
            enabled = canGenerate,
            shape = RoundedCornerShape(Radius.md),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generateButton")
        ) {
            Icon(Icons.Filled.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text("Generar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        if (remainingGenerations != null) {
            Text(
                text = if (remainingGenerations == 0) "Llegaste al límite de imágenes por hoy"
                else "Te quedan $remainingGenerations imágenes hoy",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp)
            )
        }
    }
}

fun PartCategory.icon(): ImageVector = when (this) {
    PartCategory.COLOR -> Icons.Filled.FormatPaint
    PartCategory.RINES -> Icons.Filled.TireRepair
    PartCategory.AERO -> Icons.Filled.Air
    PartCategory.SUSPENSION -> Icons.Filled.Height
    PartCategory.ESCAPE -> Icons.Filled.Speed
    PartCategory.ADMISION -> Icons.Filled.FilterAlt
    PartCategory.ECU -> Icons.Filled.Memory
    PartCategory.LUCES -> Icons.Filled.Lightbulb
}
