package com.example.motormates.ui.aiTuning.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.Vehicle
import com.example.motormates.ui.common.components.ShimmerAsyncImage
import com.example.motormates.ui.theme.Radius

/**
 * "¿Qué carro quieres modificar?": se abre al entrar a AI Tuning sin un
 * carro (desde el menú "+" o la tarjeta del feed). Los carros propios van
 * primero; el resto del catálogo debajo, con buscador y filtro por tipo.
 */
@Composable
fun VehiclePickerContent(
    myVehicles: List<Vehicle>,
    otherVehicles: List<Vehicle>,
    categories: List<String>,
    query: String,
    selectedCategory: String?,
    isLoading: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onPick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error != null -> Column(
            modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(error, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) { Text("Reintentar") }
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize().testTag("vehiclePicker"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            fullWidth {
                Column {
                    Text(
                        "¿Qué carro quieres modificar?",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Elige uno de los tuyos o cualquiera de la comunidad.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        placeholder = { Text("Buscar marca o modelo") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("vehiclePickerSearch")
                    )
                    if (categories.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems(categories) { category ->
                                FilterChip(
                                    selected = category == selectedCategory,
                                    onClick = { onSelectCategory(category) },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }
                }
            }

            if (myVehicles.isNotEmpty()) {
                fullWidth { SectionLabel("Mis carros") }
                items(myVehicles, key = { "mine_${it.id}" }) { vehicle ->
                    VehiclePickCard(vehicle, isMine = true, onClick = { onPick(vehicle.id) })
                }
            }
            if (otherVehicles.isNotEmpty()) {
                fullWidth { SectionLabel(if (myVehicles.isEmpty()) "Carros de la comunidad" else "Más carros") }
                items(otherVehicles, key = { it.id }) { vehicle ->
                    VehiclePickCard(vehicle, isMine = false, onClick = { onPick(vehicle.id) })
                }
            }
            if (myVehicles.isEmpty() && otherVehicles.isEmpty()) {
                fullWidth {
                    Text(
                        "No encontramos carros con esa búsqueda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun VehiclePickCard(vehicle: Vehicle, isMine: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
            .testTag("pickVehicle_${vehicle.id}")
    ) {
        ShimmerAsyncImage(
            model = vehicle.imageUrl,
            contentDescription = "${vehicle.brand} ${vehicle.model}",
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
        )
        Column(Modifier.padding(10.dp)) {
            Text(
                "${vehicle.brand} ${vehicle.model}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (vehicle.imageUrl == null) "Sin foto" else "${vehicle.year} · ${vehicle.category}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}
