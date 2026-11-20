package com.example.motormates.ui.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.ui.common.components.ShimmerAsyncImage
import com.example.motormates.data.model.Interest
import com.example.motormates.data.repository.CarModelOption
import com.example.motormates.data.repository.CarModelSource
import com.example.motormates.data.util.CityCoordinates
import com.example.motormates.data.util.SuggestedUser
import com.example.motormates.data.util.userLabel
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

// ---------------------------------------------------------------- Categorías

@Composable
fun CategoryPicker(
    options: List<String>,
    selected: Set<String>,
    images: Map<String, String?>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { category ->
                    CategoryCard(
                        category = category,
                        imageUrl = images[category],
                        isSelected = category in selected,
                        onClick = { onToggle(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: String,
    imageUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Radius.lg)
    Box(
        modifier = modifier
            .aspectRatio(1.25f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .testTag("onboardingCategory_$category")
    ) {
        if (imageUrl != null) {
            ShimmerAsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )
        }
        // Degradado para que el nombre se lea sobre cualquier foto.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
        )
        Text(
            text = category,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.sm)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Seleccionado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.xs)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

// ---------------------------------------------------------------- Marcas

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandPicker(
    query: String,
    onQueryChange: (String) -> Unit,
    brands: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Buscar marca") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboardingBrandSearch")
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        if (brands.isEmpty()) {
            Text(
                text = "No encontramos esa marca.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            brands.take(MAX_VISIBLE_BRANDS).forEach { brand ->
                SelectChip(
                    label = brand,
                    isSelected = brand in selected,
                    onClick = { onToggle(brand) },
                    testTag = "onboardingBrand_$brand"
                )
            }
        }
    }
}

private const val MAX_VISIBLE_BRANDS = 40

@Composable
private fun SelectChip(label: String, isSelected: Boolean, onClick: () -> Unit, testTag: String) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.testTag(testTag)
    )
}

// ---------------------------------------------------------------- Tu carro

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarPicker(
    hasCar: Boolean?,
    onHasCarChange: (Boolean) -> Unit,
    brand: String,
    onBrandChange: (String) -> Unit,
    brandOptions: List<String>,
    model: String,
    onModelChange: (String) -> Unit,
    modelOptions: List<CarModelOption>,
    isLoadingModels: Boolean,
    year: String,
    onYearChange: (String) -> Unit,
    isYearValid: Boolean,
    categories: List<String>,
    selectedCategory: String?,
    onCategoryChange: (String) -> Unit,
    alreadyInGarage: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ChoiceCard(
                title = "Sí, tengo",
                icon = Icons.Default.DirectionsCar,
                isSelected = hasCar == true,
                onClick = { onHasCarChange(true) },
                testTag = "onboardingHasCarYes",
                modifier = Modifier.weight(1f)
            )
            ChoiceCard(
                title = "Todavía no",
                icon = Icons.Default.Search,
                isSelected = hasCar == false,
                onClick = { onHasCarChange(false) },
                testTag = "onboardingHasCarNo",
                modifier = Modifier.weight(1f)
            )
        }

        if (hasCar != true) return@Column

        Spacer(modifier = Modifier.height(Spacing.lg))
        OutlinedTextField(
            value = brand,
            onValueChange = onBrandChange,
            singleLine = true,
            label = { Text("Marca") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboardingCarBrand")
        )
        // Sugerencias mientras escribe, solo si todavía no coincide exacto.
        val brandMatches = brandOptions
            .filter { brand.isNotBlank() && it.contains(brand.trim(), ignoreCase = true) && !it.equals(brand.trim(), ignoreCase = true) }
            .take(MAX_SUGGESTIONS)
        if (brandMatches.isNotEmpty()) {
            SuggestionRow(brandMatches.map { it to null }, onPick = onBrandChange)
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            singleLine = true,
            label = { Text("Modelo") },
            trailingIcon = if (isLoadingModels) {
                { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboardingCarModel")
        )
        if (modelOptions.isNotEmpty()) {
            SuggestionRow(
                modelOptions.map { it.name to (if (it.source == CarModelSource.VPIC) "vPIC" else null) },
                onPick = onModelChange
            )
            Text(
                text = "Modelos de Colombia y de la base pública vPIC (NHTSA).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = year,
            onValueChange = onYearChange,
            singleLine = true,
            label = { Text("Año") },
            isError = year.length == 4 && !isYearValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboardingCarYear")
        )

        Spacer(modifier = Modifier.height(Spacing.md))
        Text("¿Qué tipo de carro es?", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(Spacing.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            categories.forEach { category ->
                SelectChip(
                    label = category,
                    isSelected = category == selectedCategory,
                    onClick = { onCategoryChange(category) },
                    testTag = "onboardingCarCategory_$category"
                )
            }
        }

        if (alreadyInGarage) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Ya está en tu garaje: no lo agregaremos de nuevo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

private const val MAX_SUGGESTIONS = 8

/** Fila de sugerencias tocables; el segundo valor es una etiqueta opcional de origen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionRow(options: List<Pair<String, String?>>, onPick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.padding(top = Spacing.xxs)
    ) {
        options.forEach { (value, badge) ->
            SuggestionChip(
                onClick = { onPick(value) },
                label = { Text(if (badge == null) value else "$value · $badge") },
                modifier = Modifier.testTag("onboardingSuggestion_$value")
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val shape = RoundedCornerShape(Radius.lg)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(Spacing.md)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (description != null) {
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------- Intereses

@Composable
fun InterestPicker(
    selected: Set<Interest>,
    onToggle: (Interest) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Interest.entries.forEach { interest ->
            ChoiceCard(
                title = interest.label,
                description = interest.description,
                icon = interest.icon(),
                isSelected = interest in selected,
                onClick = { onToggle(interest) },
                testTag = "onboardingInterest_${interest.name}",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun Interest.icon(): ImageVector = when (this) {
    Interest.BUY_RESEARCH -> Icons.Default.RateReview
    Interest.MODIFY -> Icons.Default.Build
    Interest.SOCIAL -> Icons.Default.Groups
    Interest.PHOTO -> Icons.Default.CameraAlt
}

// ---------------------------------------------------------------- Ciudad

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CityPicker(
    selected: String?,
    onSelect: (String) -> Unit,
    isLocating: Boolean,
    onUseMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedButton(
            onClick = onUseMyLocation,
            enabled = !isLocating,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboardingUseLocation")
        ) {
            if (isLocating) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text("Usar mi ubicación")
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            CityCoordinates.CITIES.forEach { city ->
                SelectChip(
                    label = city.name,
                    isSelected = city.name == selected,
                    onClick = { onSelect(city.name) },
                    testTag = "onboardingCity_${city.name}"
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Personas

@Composable
fun SuggestedPeople(
    suggestions: List<SuggestedUser>,
    followed: Set<String>,
    isLoading: Boolean,
    onToggleFollow: (String) -> Unit,
    onFollowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when {
            isLoading -> Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            suggestions.isEmpty() -> Text(
                text = "Todavía no hay personas para sugerirte. Cuando publiques o explores, aparecerán aquí.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                val allFollowed = suggestions.all { it.userId in followed }
                OutlinedButton(
                    onClick = onFollowAll,
                    enabled = !allFollowed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboardingFollowAll")
                ) {
                    Text(if (allFollowed) "Ya sigues a todos" else "Seguir a todos")
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                suggestions.forEach { user ->
                    SuggestedUserRow(
                        user = user,
                        isFollowed = user.userId in followed,
                        onToggleFollow = { onToggleFollow(user.userId) }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedUserRow(
    user: SuggestedUser,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(name = user.name, avatarUrl = user.avatarUrl)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.reason.userLabel(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
            Text(
                text = "${user.reviewsCount} reseñas · ${user.likesReceived} likes",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        if (isFollowed) {
            OutlinedButton(onClick = onToggleFollow, modifier = Modifier.testTag("follow_${user.userId}")) {
                Text("Siguiendo")
            }
        } else {
            Button(onClick = onToggleFollow, modifier = Modifier.testTag("follow_${user.userId}")) {
                Text("Seguir")
            }
        }
    }
}

@Composable
private fun PersonAvatar(name: String, avatarUrl: String?) {
    val modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (avatarUrl != null) {
        AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
