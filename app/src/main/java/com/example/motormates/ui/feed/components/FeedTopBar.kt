package com.example.motormates.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.motormates.R

@Composable
fun FeedTopBar(
    onAlertsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** Lleva al buscador de Explorar (el mismo de la barra inferior). */
    onSearchClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.feed_top_bar_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.Search,
            contentDescription = stringResource(R.string.feed_search_cd),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .clickable(onClick = onSearchClick)
                .testTag("feedSearchButton")
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            Icons.Filled.Notifications,
            contentDescription = stringResource(R.string.feed_alerts_cd),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable(onClick = onAlertsClick)
        )
    }
}
