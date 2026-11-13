package com.example.motormates.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
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
 * Caso de uso del mapa de reseñas recientes:
 *
 * 1. El usuario hace login.
 * 2. Entra a la pestaña "Mapa" del home.
 * 3. El mapa carga y muestra marcadores: las seis reseñas del seed tienen
 *    ubicación y son de hace menos de 24 horas.
 *
 * Lo que NO se prueba acá: tocar un marcador. Los marcadores los dibuja el
 * SDK de Maps sobre una superficie de OpenGL, así que no son nodos de Compose
 * y ni Compose ni Espresso pueden tocarlos. Esa parte (marcador -> tarjeta)
 * está cubierta por ReviewsMapViewModelTest.onMarkerClick.
 *
 * Lo que sí prueba, y es lo que importa: que la consulta a Firestore, el
 * filtro de 24 horas y el mapeo a marcadores funcionen de punta a punta. Si
 * cualquiera de esos pasos se comiera las reseñas, aparecería el mensaje de
 * "no hay reseñas con ubicación" y la prueba fallaría.
 *
 * Requiere el emulador de Firebase corriendo.
 */
@RunWith(AndroidJUnit4::class)
class ReviewsMapE2E {

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
    fun loginYVerLasReseniasRecientesEnElMapa() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // --- Login ---
        composeRule.waitForTag("loginScreen")
        composeRule.onNodeWithTag("loginEmail").performTextInput(E2ESeed.MEMBER_EMAIL)
        composeRule.onNodeWithTag("loginPassword").performTextInput(FirebaseEmulator.PASSWORD)
        composeRule.onNodeWithTag("loginSubmit").performClick()
        composeRule.waitForTag("feedScreen")

        // --- A la pestaña del mapa ---
        composeRule.onNodeWithTag("feedTab_mapa").performClick()
        composeRule.waitForTag("reviewsMapScreen")
        composeRule.onNodeWithTag("reviewsMapScreen").assertIsDisplayed()

        // El feed deja de pintarse: el mapa no vive dentro de su LazyColumn.
        composeRule.onNodeWithTag("feedScreen").assertDoesNotExist()

        // --- El mapa termina de cargar y tiene marcadores ---
        // Primero se espera a que se vaya el spinner: comprobar "no está
        // vacío" mientras carga pasaría por la razón equivocada.
        composeRule.waitForTagToVanish("reviewsMapLoading")

        // Sin el mensaje de vacío ni el de error, hay marcadores en el mapa.
        composeRule.onNodeWithTag("reviewsMapEmpty").assertDoesNotExist()
        composeRule.onNodeWithTag("reviewsMapError").assertDoesNotExist()

        // --- Y se puede volver al feed ---
        composeRule.onNodeWithTag("feedTab_explorar").performClick()
        composeRule.waitForTag("feedScreen")
    }

    /**
     * El lugar de la reseña se muestra junto al "hace X" al entrar a la
     * publicación. Se busca por subcadena porque el texto completo lleva el
     * tiempo relativo, que cambia según cuándo corra la prueba.
     */
    @Test
    fun laResenaDelDetalleMuestraDondeSePublico() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitForTag("loginScreen")
        composeRule.onNodeWithTag("loginEmail").performTextInput(E2ESeed.MEMBER_EMAIL)
        composeRule.onNodeWithTag("loginPassword").performTextInput(FirebaseEmulator.PASSWORD)
        composeRule.onNodeWithTag("loginSubmit").performClick()
        composeRule.waitForTag("feedScreen")

        val tituloPublicacion = "${E2ESeed.BRAND} ${E2ESeed.MODEL}"
        composeRule.waitForText(tituloPublicacion)
        composeRule.onNodeWithText(tituloPublicacion).performClick()
        composeRule.waitForTag("vehicleDetailScreen")

        composeRule.waitForText(E2ESeed.REVIEW_PLACE_NAME, substring = true)
    }
}
