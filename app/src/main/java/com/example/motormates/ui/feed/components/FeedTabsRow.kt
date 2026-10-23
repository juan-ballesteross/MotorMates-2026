package com.example.motormates.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.motormates.data.model.FeedTab
import com.example.motormates.ui.theme.Spacing

/**
 * El filtro del Home: "Explorar" muestra el catálogo de carros y
 * "Siguiendo" solo las reseñas de los usuarios que sigo.
 */
@Composable
fun FeedTabsRow(
    selectedTab: FeedTab,
    onTabClick: (FeedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        FeedTab.entries.forEach { tab ->
            FilterChip(
                selected = tab == selectedTab,
                onClick = { onTabClick(tab) },
                label = { Text(text = tab.label) }
            )
        }
    }
}

private val FeedTab.label: String
    get() = when (this) {
        FeedTab.EXPLORAR -> "Explorar"
        FeedTab.SIGUIENDO -> "Siguiendo"
    }
