package com.example.motormates.ui.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary
import com.example.motormates.ui.user.model.ProfileTab

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileTab.entries.forEach { tab ->
                val label = when (tab) {
                    ProfileTab.REVIEWS -> stringResource(R.string.user_tab_reviews)
                    ProfileTab.GARAGE -> stringResource(R.string.user_tab_garage)
                    ProfileTab.ACTIVITY -> stringResource(R.string.user_tab_activity)
                }
                ProfileTabItem(
                    label = label,
                    isSelected = tab == selected,
                    onClick = { onSelect(tab) }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MotorMatesSurface)
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
        modifier = modifier
            .width(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MotorMatesTextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) MotorMatesRed else Color.Transparent)
        )
    }
}
