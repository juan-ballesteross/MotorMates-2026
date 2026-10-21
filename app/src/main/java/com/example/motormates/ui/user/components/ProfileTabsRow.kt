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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.ui.theme.Spacing

/**
 * Pestañas Reseñas / Garaje / Actividad con indicador rojo bajo la
 * pestaña seleccionada.
 */
@Composable
fun ProfileTabsRow(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Cada pestaña ocupa un tercio del ancho; el divisor gris de abajo
        // y el indicador rojo de la seleccionada quedan alineados.
        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileTab.entries.forEach { tab ->
                val label = when (tab) {
                    ProfileTab.REVIEWS -> stringResource(R.string.user_tab_reviews)
                    ProfileTab.GARAGE -> stringResource(R.string.user_tab_garage)
                    ProfileTab.ACTIVITY -> stringResource(R.string.user_tab_activity)
                }
                ProfileTabItem(
                    label = label,
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
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
