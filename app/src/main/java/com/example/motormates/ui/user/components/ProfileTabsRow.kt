package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.ui.theme.Spacing

/**
 * Pestañas Reseñas / Garaje / Builds / Actividad con indicador rojo bajo la
 * pestaña seleccionada.
 */
@Composable
fun ProfileTabsRow(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
    /** Por defecto sin "Eventos": esa pestaña solo la ve el dueño del perfil. */
    tabs: List<ProfileTab> = ProfileTab.entries - ProfileTab.EVENTS
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Cada pestaña ocupa la misma fracción del ancho; el divisor gris de abajo
        // y el indicador rojo de la seleccionada quedan alineados.
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val label = when (tab) {
                    ProfileTab.REVIEWS -> stringResource(R.string.user_tab_reviews)
                    ProfileTab.GARAGE -> stringResource(R.string.user_tab_garage)
                    ProfileTab.BUILDS -> stringResource(R.string.user_tab_builds)
                    ProfileTab.EVENTS -> stringResource(R.string.user_tab_events)
                    ProfileTab.ACTIVITY -> stringResource(R.string.user_tab_activity)
                }
                ProfileTabItem(
                    label = label,
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                    // Con cinco pestañas, letra un poco menor para que quepan.
                    compact = tabs.size > 4,
                    modifier = Modifier.weight(1f).testTag("profileTab_${tab.name.lowercase()}")
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun ProfileTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (compact) 13.sp else 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(vertical = Spacing.sm)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}
