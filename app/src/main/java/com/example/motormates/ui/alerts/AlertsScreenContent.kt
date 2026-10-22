package com.example.motormates.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.NotificationUi
import com.example.motormates.ui.alerts.components.NotificationItem
import com.example.motormates.ui.alerts.components.NotificationSectionHeader

/**
 * Stateless: todo llega por parámetros. Agrupa las notificaciones por
 * "section" (HOY / ESTA SEMANA) preservando el orden en el que vienen
 * en la lista, y las pinta dentro de un único LazyColumn (header +
 * items intercalados, sin anidar otro lazy adentro).
 */
@Composable
fun AlertsScreenContent(
    notifications: List<NotificationUi>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onNotificationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = notifications.groupBy { it.section }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        item {
            Text(
                text = "Notificaciones",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
            )
        }

        when {
            isLoading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            notifications.isEmpty() -> item {
                Text(
                    text = "Todavía no tienes notificaciones",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            else -> grouped.forEach { (section, itemsInSection) ->
                item {
                    NotificationSectionHeader(titulo = section)
                }
                items(itemsInSection) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = { onNotificationClick(notification.id) }
                    )
                }
            }
        }
    }
}