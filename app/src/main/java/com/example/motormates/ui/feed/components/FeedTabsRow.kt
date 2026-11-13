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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.motormates.R
import com.example.motormates.data.model.FeedTab
import com.example.motormates.ui.theme.Spacing

/**
 * El filtro del Home: "Explorar" muestra el catálogo de carros, "Siguiendo"
 * solo las reseñas de los usuarios que sigo y "Mapa" las reseñas de las
 * últimas 24 horas sobre un mapa.
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
                label = { Text(text = stringResource(tab.labelRes)) },
                modifier = Modifier.testTag("feedTab_${tab.name.lowercase()}")
            )
        }
    }
}

private val FeedTab.labelRes: Int
    get() = when (this) {
        FeedTab.EXPLORAR -> R.string.feed_tab_explore
        FeedTab.SIGUIENDO -> R.string.feed_tab_following
        FeedTab.MAPA -> R.string.feed_tab_map
    }
