package com.example.motormates.ui.user

import com.example.motormates.ui.common.components.AddGarageVehicleButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.data.model.ActivityUi
import com.example.motormates.data.model.Build
import com.example.motormates.data.model.GarageVehicleUi
import com.example.motormates.data.model.MyEvent
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.data.model.UserReviewUi
import com.example.motormates.ui.common.components.GarageEmptyState
import com.example.motormates.ui.common.components.GarageVehicleCard
import com.example.motormates.ui.user.components.ActivityRow
import com.example.motormates.ui.user.components.ActivitySectionHeader
import com.example.motormates.ui.user.components.ProfileBuildCard
import com.example.motormates.ui.user.components.ProfileEmptyTabState
import com.example.motormates.ui.user.components.ProfileHeader
import com.example.motormates.ui.user.components.ProfileInfoSection
import com.example.motormates.ui.user.components.ProfileReviewCard
import com.example.motormates.ui.user.components.ProfileStatsRow
import com.example.motormates.ui.user.components.ProfileEventCard
import com.example.motormates.ui.user.components.ProfileTabsRow
import com.example.motormates.ui.theme.Spacing

/**
 * Contenido scrollable de la pantalla de perfil. Se implementa como un
 * único LazyVerticalGrid (encabezado a ancho completo + grid de 2
 * columnas para el garaje) para evitar anidar un scroll dentro de otro.
 */
@Composable
fun UserScreenContent(
    profile: UserProfile,
    profileImageUrl: String?,
    coverImageUrl: String?,
    reviews: List<UserReviewUi>,
    isLoading: Boolean,
    errorMessage: String?,
    cars: List<GarageVehicleUi>,
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    onEditProfileClick: () -> Unit,
    onReviewClick: (String) -> Unit = {},
    onEditReviewClick: (String) -> Unit = {},
    onDeleteReviewClick: (String) -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onLogOutClick: () -> Unit = {},
    onGarageCarClick: (String) -> Unit = {},
    onPublishVehicleClick: () -> Unit = {},
    activity: List<ActivityUi> = emptyList(),
    isActivityLoading: Boolean = false,
    activityError: String? = null,
    onActivityClick: (ActivityUi) -> Unit = {},
    onRetryActivity: () -> Unit = {},
    builds: List<Build> = emptyList(),
    isBuildsLoading: Boolean = false,
    onBuildClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    myEvents: List<MyEvent> = emptyList(),
    isEventsLoading: Boolean = false,
    eventsError: String? = null,
    nowMillis: Long = 0L,
    onEventClick: (String) -> Unit = {},
    onRetryEvents: () -> Unit = {},
    onCreateEventClick: () -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileHeader(
                profileImageUrl = profileImageUrl,
                coverImageUrl = coverImageUrl,
                onEditProfileClick = onEditProfileClick
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileInfoSection(profile = profile)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileStatsRow(
                profile = profile,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            // El perfil propio tiene además la pestaña Eventos.
            ProfileTabsRow(selected = selectedTab, onSelect = onSelectTab, tabs = ProfileTab.entries)
        }
        when (selectedTab) {
            // Todo lo que hice, de lo más reciente a lo más viejo, agrupado
            // por Hoy / Esta semana / Antes.
            ProfileTab.ACTIVITY -> when {
                isActivityLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                activityError != null && activity.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = activityError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        TextButton(onClick = onRetryActivity) { Text("Reintentar") }
                    }
                }

                activity.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
                }

                else -> activity.groupBy { it.section }.forEach { (section, items) ->
                    item(key = "activity_header_$section", span = { GridItemSpan(maxLineSpan) }) {
                        ActivitySectionHeader(title = section)
                    }
                    gridItems(items, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { item ->
                        ActivityRow(item = item, onClick = { onActivityClick(item) })
                    }
                }
            }
            // Solo las reseñas que yo hice, traídas de GET /users/{id}/reviews.
            ProfileTab.REVIEWS -> when {
                isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        // Sin esto el usuario queda atrapado: el "Cerrar
                        // sesión" normal está dentro de Editar perfil, al que
                        // solo se llega desde esta misma pantalla.
                        Button(
                            onClick = onLogOutClick,
                            modifier = Modifier.padding(top = Spacing.md)
                        ) {
                            Text(text = stringResource(R.string.edit_profile_logout_button))
                        }
                    }
                }

                reviews.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
                }

                else -> gridItems(
                    items = reviews,
                    key = { it.id },
                    span = { GridItemSpan(maxLineSpan) }
                ) { review ->
                    ProfileReviewCard(
                        review = review,
                        onClick = onReviewClick,
                        isEditable = true,
                        onEditClick = onEditReviewClick,
                        onDeleteClick = onDeleteReviewClick
                    )
                }
            }
            // Los que organizo o a los que dije "voy" / "me interesa".
            ProfileTab.EVENTS -> when {
                isEventsLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                eventsError != null && myEvents.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = eventsError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        TextButton(onClick = onRetryEvents) { Text("Reintentar") }
                    }
                }
                myEvents.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aún no tienes eventos. Márcate \"Voy\" o \"Me interesa\" en uno del mapa, o crea el tuyo.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onCreateEventClick) { Text("Crear un evento") }
                    }
                }
                else -> {
                    val (upcoming, past) = myEvents.partition { (it.event.endAt ?: it.event.startAt) >= nowMillis }
                    if (upcoming.isNotEmpty()) {
                        item(key = "events_upcoming", span = { GridItemSpan(maxLineSpan) }) { ActivitySectionHeader(title = "Próximos") }
                        gridItems(upcoming, key = { "event_${it.event.id}" }, span = { GridItemSpan(maxLineSpan) }) { item ->
                            ProfileEventCard(item = item, isPast = false, onClick = { onEventClick(item.event.id) })
                        }
                    }
                    if (past.isNotEmpty()) {
                        item(key = "events_past", span = { GridItemSpan(maxLineSpan) }) { ActivitySectionHeader(title = "Pasados") }
                        // Los más recientes primero.
                        gridItems(past.reversed(), key = { "event_${it.event.id}" }, span = { GridItemSpan(maxLineSpan) }) { item ->
                            ProfileEventCard(item = item, isPast = true, onClick = { onEventClick(item.event.id) })
                        }
                    }
                }
            }
            // Mis builds de AI Tuning, públicos y privados.
            ProfileTab.BUILDS -> when {
                isBuildsLoading && builds.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                builds.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Todavía no tienes builds. Abre un vehículo y toca \"Modificar con AI\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.lg)
                    )
                }
                else -> gridItems(builds, key = { it.id }) { build ->
                    ProfileBuildCard(build = build, onClick = onBuildClick, showVisibility = true)
                }
            }
            // El garaje se queda local: el backend no modela los autos del usuario.
            // Los vehículos que publiqué desde la pestaña Publicar.
            ProfileTab.GARAGE -> if (cars.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GarageEmptyState(
                        message = "Todavía no has publicado ningún carro",
                        onPublishClick = onPublishVehicleClick
                    )
                }
            } else {
                item(key = "garageAdd", span = { GridItemSpan(maxLineSpan) }) {
                    AddGarageVehicleButton(onClick = onPublishVehicleClick)
                }
                gridItems(cars, key = { "garage_${it.id}" }) { car ->
                    GarageVehicleCard(car = car, onClick = { onGarageCarClick(car.id) })
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
