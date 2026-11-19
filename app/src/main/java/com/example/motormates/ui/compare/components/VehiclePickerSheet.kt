package com.example.motormates.ui.compare.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.data.model.CompareSide
import com.example.motormates.ui.compare.PickerOwner
import com.example.motormates.ui.compare.VehiclePickerItem
import com.example.motormates.ui.compare.VehiclePickerState
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing
import com.example.motormates.ui.theme.pressScaleClickable

/**
 * Hoja para elegir con qué carro comparar, en dos pasos: primero la persona
 * y después uno de sus carros. Todo el estado (paso, búsqueda, lista) viene
 * del ViewModel: aquí no hay remember de datos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclePickerSheet(
    picker: VehiclePickerState,
    onQueryChange: (String) -> Unit,
    onOwnerSelected: (String) -> Unit,
    onBackToOwners: () -> Unit,
    onVehicleSelected: (String) -> Unit,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val owner = picker.selectedOwner
    val isOwnerStep = picker.selectedOwnerKey == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("comparePickerSheet")
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
            if (isOwnerStep) {
                Text(
                    text = if (picker.target == CompareSide.LEFT) "¿De quién es el primer carro?" else "¿Con quién quieres comparar?",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Personas que sigues",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                // Paso 2: flecha para volver a la lista de personas.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToOwners, modifier = Modifier.testTag("comparePickerBack")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a personas")
                    }
                    if (owner != null) OwnerAvatar(owner = owner, size = 32)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = when (owner?.kind) {
                            PickerOwner.Kind.MINE -> "Tus carros"
                            PickerOwner.Kind.CATALOG -> "Catálogo de MotorMates"
                            else -> "Carros de ${owner?.name ?: ""}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            picker.notice?.let { notice ->
                Text(
                    text = notice,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = Spacing.xxs)
                        .testTag("comparePickerNotice")
                )
            }

            OutlinedTextField(
                value = picker.query,
                onValueChange = onQueryChange,
                placeholder = { Text(if (isOwnerStep) "Buscar persona" else "Buscar marca o modelo") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(Radius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm)
                    .testTag("comparePickerSearch")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 480.dp)
            ) {
                when {
                    picker.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    picker.errorMessage != null -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = picker.errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        TextButton(onClick = onRetryClick) { Text("Reintentar") }
                    }

                    isOwnerStep -> OwnersList(
                        owners = picker.owners,
                        query = picker.query,
                        onOwnerSelected = onOwnerSelected,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> VehiclesList(
                        vehicles = picker.ownerVehicles,
                        query = picker.query,
                        onVehicleSelected = onVehicleSelected,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnersList(
    owners: List<PickerOwner>,
    query: String,
    onOwnerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (owners.isEmpty()) {
        EmptyText(
            text = if (query.isBlank()) {
                "Ninguna de las personas que sigues tiene carros todavía. Sigue a alguien desde Explorar o desde su perfil."
            } else {
                "No sigues a nadie que se llame “${query.trim()}”."
            },
            modifier = modifier
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        items(owners, key = { it.key }) { owner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.sm))
                    .pressScaleClickable { onOwnerSelected(owner.key) }
                    .padding(vertical = Spacing.xs)
                    .testTag("comparePickerOwner_${owner.key}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OwnerAvatar(owner = owner, size = 44)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = owner.name,
                        fontWeight = if (owner.kind == PickerOwner.Kind.MINE) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (owner.carCount == 1) "1 carro" else "${owner.carCount} carros",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VehiclesList(
    vehicles: List<VehiclePickerItem>,
    query: String,
    onVehicleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vehicles.isEmpty()) {
        EmptyText(
            text = if (query.isBlank()) "No tiene otros carros para comparar."
            else "Ningún carro coincide con “${query.trim()}”.",
            modifier = modifier
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        items(vehicles, key = { it.id }) { item ->
            PickerRow(item = item, onClick = { onVehicleSelected(item.id) })
        }
    }
}

@Composable
private fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/** Foto de la persona; "Tus carros" y el catálogo llevan un ícono en su lugar. */
@Composable
private fun OwnerAvatar(owner: PickerOwner, size: Int) {
    val avatarModifier = Modifier
        .size(size.dp)
        .clip(CircleShape)
        .background(
            if (owner.kind == PickerOwner.Kind.MINE) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    when {
        owner.avatarUrl != null -> AsyncImage(
            model = owner.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
        else -> Box(modifier = avatarModifier, contentAlignment = Alignment.Center) {
            val (icon, initial) = ownerPlaceholder(owner)
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (owner.kind == PickerOwner.Kind.MINE) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size((size / 2).dp)
                )
            } else {
                Text(text = initial, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp)
            }
        }
    }
}

private fun ownerPlaceholder(owner: PickerOwner): Pair<ImageVector?, String> = when (owner.kind) {
    PickerOwner.Kind.MINE -> Icons.Filled.Garage to ""
    PickerOwner.Kind.CATALOG -> Icons.Filled.DirectionsCar to ""
    PickerOwner.Kind.USER -> null to owner.name.trim().take(1).uppercase()
}

@Composable
private fun PickerRow(item: VehiclePickerItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = Spacing.xs)
            .testTag("comparePickerItem_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        val thumbModifier = Modifier
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        if (item.imageUrl != null) {
            AsyncImage(model = item.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = thumbModifier)
        } else {
            Box(modifier = thumbModifier, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.DirectionsCar, contentDescription = null)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
        }
    }
}
