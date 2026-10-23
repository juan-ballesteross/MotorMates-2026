package com.example.motormates.ui.user

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.motormates.R
import com.example.motormates.data.model.GarageCar
import com.example.motormates.data.model.ProfileTab
import com.example.motormates.data.model.UserProfile
import com.example.motormates.data.model.UserReviewUi
import com.example.motormates.ui.user.components.ProfileCarCard
import com.example.motormates.ui.user.components.ProfileEmptyTabState
import com.example.motormates.ui.user.components.ProfileHeader
import com.example.motormates.ui.user.components.ProfileInfoSection
import com.example.motormates.ui.user.components.ProfileReviewCard
import com.example.motormates.ui.user.components.ProfileStatsRow
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
    cars: List<GarageCar>,
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    onEditProfileClick: () -> Unit,
    onReviewClick: (String) -> Unit = {},
    onEditReviewClick: (String) -> Unit = {},
    onDeleteReviewClick: (String) -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onLogOutClick: () -> Unit = {},
    modifier: Modifier = Modifier
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
            ProfileTabsRow(selected = selectedTab, onSelect = onSelectTab)
        }
        when (selectedTab) {
            ProfileTab.ACTIVITY -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileEmptyTabState()
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
            // El garaje se queda local: el backend no modela los autos del usuario.
            ProfileTab.GARAGE -> {
                gridItems(cars) { car ->
                    ProfileCarCard(car)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
