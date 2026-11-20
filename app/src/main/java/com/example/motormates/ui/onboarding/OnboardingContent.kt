package com.example.motormates.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.Interest
import com.example.motormates.ui.onboarding.components.BrandPicker
import com.example.motormates.ui.onboarding.components.CategoryPicker
import com.example.motormates.ui.onboarding.components.CityPicker
import com.example.motormates.ui.onboarding.components.InterestPicker
import com.example.motormates.ui.onboarding.components.SuggestedPeople
import com.example.motormates.ui.theme.Spacing

/** Todo lo que la pantalla puede pedirle al ViewModel. Por defecto no hace nada (útil en pruebas). */
data class OnboardingActions(
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onToggleCategory: (String) -> Unit = {},
    val onBrandQueryChange: (String) -> Unit = {},
    val onToggleBrand: (String) -> Unit = {},
    val onToggleInterest: (Interest) -> Unit = {},
    val onSelectCity: (String) -> Unit = {},
    val onUseMyLocation: () -> Unit = {},
    val onToggleFollow: (String) -> Unit = {},
    val onFollowAll: () -> Unit = {}
)

@Composable
fun OnboardingContent(
    state: OnboardingState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .testTag("onboardingScreen")
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTag("onboardingLoading"))
            }
            return@Column
        }

        TopBar(state = state, actions = actions)
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .testTag("onboardingProgress")
        )

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            label = "onboardingStep",
            modifier = Modifier.weight(1f)
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.lg)
            ) {
                Text(
                    text = step.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("onboardingTitle")
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = step.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                StepBody(step = step, state = state, actions = actions)
            }
        }

        BottomBar(state = state, actions = actions)
    }
}

@Composable
private fun TopBar(state: OnboardingState, actions: OnboardingActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            !state.isFirstStep -> IconButton(onClick = actions.onBack, modifier = Modifier.testTag("onboardingBack")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            state.isEditMode -> IconButton(onClick = actions.onClose, modifier = Modifier.testTag("onboardingClose")) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
            else -> Spacer(modifier = Modifier.size(48.dp))
        }
        Text(
            text = "${state.stepIndex + 1} de ${state.steps.size}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        if (!state.isEditMode) {
            TextButton(
                onClick = actions.onSkip,
                enabled = !state.isSaving,
                modifier = Modifier.testTag("onboardingSkip")
            ) {
                Text("Ahora no")
            }
        }
    }
}

@Composable
private fun StepBody(step: OnboardingStep, state: OnboardingState, actions: OnboardingActions) {
    when (step) {
        OnboardingStep.CATEGORIES -> CategoryPicker(
            options = state.categoryOptions,
            selected = state.categories,
            images = state.categoryImages,
            onToggle = actions.onToggleCategory
        )
        OnboardingStep.BRANDS -> BrandPicker(
            query = state.brandQuery,
            onQueryChange = actions.onBrandQueryChange,
            brands = state.visibleBrands,
            selected = state.brands,
            onToggle = actions.onToggleBrand
        )
        OnboardingStep.INTERESTS -> InterestPicker(
            selected = state.interests,
            onToggle = actions.onToggleInterest
        )
        OnboardingStep.CITY -> CityPicker(
            selected = state.city,
            onSelect = actions.onSelectCity,
            isLocating = state.isLocating,
            onUseMyLocation = actions.onUseMyLocation
        )
        OnboardingStep.PEOPLE -> SuggestedPeople(
            suggestions = state.suggestions,
            followed = state.followed,
            isLoading = state.isLoadingSuggestions,
            onToggleFollow = actions.onToggleFollow,
            onFollowAll = actions.onFollowAll
        )
    }
}

@Composable
private fun BottomBar(state: OnboardingState, actions: OnboardingActions) {
    Column(modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md)) {
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.xs)
                    .testTag("onboardingError")
            )
        }
        Button(
            onClick = actions.onNext,
            enabled = state.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboardingNext")
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = when {
                        !state.isLastStep -> "Continuar"
                        state.isEditMode -> "Guardar"
                        else -> "Empezar"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
