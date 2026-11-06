package com.example.motormates.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.motormates.MainActivity
import com.example.motormates.integration.FirebaseEmulator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Caso de uso del usuario YA REGISTRADO:
 *
 * 1. Hace login.
 * 2. Entra al perfil de otro usuario y se verifica su información.
 * 3. Le da follow: el contador de seguidores tiene que subir.
 * 4. Vuelve al home, entra a la pestaña "Siguiendo" y tiene que aparecer al
 *    menos una publicación del usuario que acabó de seguir.
 *
 * La precondición "no sigue a nadie antes de empezar" la garantiza el
 * clearFirestore() de E2ESeed, que borra también las subcolecciones
 * followers/following. Requiere el emulador de Firebase corriendo.
 */
@RunWith(AndroidJUnit4::class)
class FollowAndFollowingFeedE2E {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        runBlocking {
            E2ESeed.seedAll()
            E2ESeed.seedMember()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking { E2ESeed.reset() }
    }

    @Test
    fun loginSeguirAUnUsuarioYVerSuPublicacionEnSiguiendo() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // --- Login ---
        composeRule.waitForTag("loginScreen")
        composeRule.onNodeWithTag("loginEmail").performTextInput(E2ESeed.MEMBER_EMAIL)
        composeRule.onNodeWithTag("loginPassword").performTextInput(FirebaseEmulator.PASSWORD)
        composeRule.onNodeWithTag("loginSubmit").performClick()
        composeRule.waitForTag("feedScreen")

        // --- Al perfil del otro usuario, desde el autor de una reseña ---
        val tituloPublicacion = "${E2ESeed.BRAND} ${E2ESeed.MODEL}"
        composeRule.waitForText(tituloPublicacion)
        composeRule.onNodeWithText(tituloPublicacion).performClick()
        composeRule.waitForTag("vehicleDetailScreen")

        // Todas las reseñas son del mismo autor, así que su nombre aparece
        // varias veces: se toca la primera. openAuthor (AppNavigation) lleva
        // al perfil público porque no es el usuario de la sesión.
        composeRule.waitForText(E2ESeed.AUTHOR_NAME)
        composeRule.onAllNodesWithText(E2ESeed.AUTHOR_NAME).onFirst().performClick()
        composeRule.waitForTag("publicProfileScreen")

        // --- Se verifica la información del usuario ---
        composeRule.onAllNodesWithText(E2ESeed.AUTHOR_NAME).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("@${E2ESeed.AUTHOR_USERNAME} · ${E2ESeed.AUTHOR_LOCATION}")
            .assertIsDisplayed()
        composeRule.onNodeWithText(E2ESeed.AUTHOR_BIO).assertIsDisplayed()
        composeRule.waitForTagText("statFollowersValue", "0")

        // --- Follow: el botón es optimista, el contador llega del listener ---
        composeRule.onNodeWithTag("followButton").performClick()
        composeRule.waitForTagText("statFollowersValue", "1")
        composeRule.onNodeWithTag("followButton").assertTextEquals("Siguiendo")

        // --- De vuelta al home, pestaña "Siguiendo" ---
        Espresso.pressBack() // del perfil al detalle
        Espresso.pressBack() // del detalle al feed
        composeRule.waitForTag("feedScreen")

        composeRule.onNodeWithTag("feedTab_siguiendo").performClick()

        // La reseña más reciente del usuario que acabó de seguir.
        val primeraPublicacion = "followingReview_${E2ESeed.FIRST_REVIEW_ID}"
        composeRule.waitForTag(primeraPublicacion)
        composeRule.onNodeWithTag(primeraPublicacion).assertIsDisplayed()
        composeRule.onAllNodesWithText(E2ESeed.AUTHOR_NAME).onFirst().assertIsDisplayed()
    }
}
