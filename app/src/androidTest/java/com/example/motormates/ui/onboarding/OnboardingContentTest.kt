package com.example.motormates.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.motormates.data.model.Interest
import com.example.motormates.data.repository.CarModelOption
import com.example.motormates.data.repository.CarModelSource
import com.example.motormates.data.util.RecommendationReason
import com.example.motormates.data.util.SuggestedUser
import com.example.motormates.ui.theme.MotorMatesTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pinta OnboardingContent con estados armados a mano (sin ViewModel ni
 * Firebase) y comprueba que cada paso muestre lo que el estado dice y que
 * los toques lleguen a las acciones.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: OnboardingState, actions: OnboardingActions = OnboardingActions()) {
        composeRule.setContent {
            MotorMatesTheme { OnboardingContent(state = state, actions = actions) }
        }
    }

    private val base = OnboardingState(isLoading = false)

    @Test
    fun primer_paso_muestra_categorias_progreso_y_saltar() {
        var toggled: String? = null
        var skipped = false
        setContent(
            base.copy(categories = setOf("SUV")),
            OnboardingActions(onToggleCategory = { toggled = it }, onSkip = { skipped = true })
        )

        composeRule.onNodeWithTag("onboardingTitle").assertTextEquals(OnboardingStep.CATEGORIES.title)
        composeRule.onNodeWithText("1 de 6").assertIsDisplayed()
        composeRule.onNodeWithTag("onboardingCategory_Deportivo").performClick()
        composeRule.onNodeWithTag("onboardingSkip").performClick()

        assertThat(toggled).isEqualTo("Deportivo")
        assertThat(skipped).isTrue()
        composeRule.onNodeWithTag("onboardingNext").assertTextEquals("Continuar")
    }

    @Test
    fun el_carro_incompleto_deshabilita_continuar() {
        setContent(
            base.copy(
                step = OnboardingStep.CAR,
                hasCar = true,
                carBrand = "Renault",
                carModel = "Dus",
                carModelOptions = listOf(CarModelOption("Duster", CarModelSource.LOCAL))
            )
        )

        composeRule.onNodeWithTag("onboardingCarBrand").assertIsDisplayed()
        composeRule.onNodeWithTag("onboardingSuggestion_Duster").assertIsDisplayed()
        composeRule.onNodeWithTag("onboardingNext").assertIsNotEnabled()
    }

    @Test
    fun el_carro_completo_habilita_continuar() {
        setContent(
            base.copy(
                step = OnboardingStep.CAR,
                hasCar = true,
                carBrand = "Renault",
                carModel = "Duster",
                carYear = "2022",
                carCategory = "SUV"
            )
        )

        composeRule.onNodeWithTag("onboardingNext").assertIsEnabled()
    }

    @Test
    fun los_intereses_llaman_a_su_accion() {
        var interest: Interest? = null
        setContent(
            base.copy(step = OnboardingStep.INTERESTS),
            OnboardingActions(onToggleInterest = { interest = it })
        )

        composeRule.onNodeWithTag("onboardingInterest_MODIFY").performClick()

        assertThat(interest).isEqualTo(Interest.MODIFY)
    }

    @Test
    fun ultimo_paso_muestra_personas_y_empezar() {
        var followed: String? = null
        setContent(
            base.copy(
                step = OnboardingStep.PEOPLE,
                suggestions = listOf(
                    SuggestedUser("u1", "Ana Gómez", null, RecommendationReason.FavoriteCategory("SUV"), 3, 12)
                )
            ),
            OnboardingActions(onToggleFollow = { followed = it })
        )

        composeRule.onNodeWithText("Ana Gómez").assertIsDisplayed()
        composeRule.onNodeWithText("Reseña SUV").assertIsDisplayed()
        composeRule.onNodeWithTag("follow_u1").performClick()
        composeRule.onNodeWithTag("onboardingNext").assertTextEquals("Empezar")

        assertThat(followed).isEqualTo("u1")
    }

    @Test
    fun al_editar_no_hay_saltar_y_el_ultimo_paso_guarda() {
        setContent(base.copy(isEditMode = true, step = OnboardingStep.CITY, city = "Cali"))

        composeRule.onNodeWithText("5 de 5").assertIsDisplayed()
        composeRule.onNodeWithTag("onboardingSkip").assertDoesNotExist()
        composeRule.onNodeWithTag("onboardingNext").assertTextEquals("Guardar")
    }
}
