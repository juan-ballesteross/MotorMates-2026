package com.example.motormates.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.motormates.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Caso de uso del usuario NUEVO, de principio a fin:
 *
 * 1. Entra a la app y se va al registro.
 * 2. Llena todo pero pone "1234" como contraseña: no puede registrarse y se
 *    verifica el mensaje de error.
 * 3. La corrige a "123456" y entra a la app.
 * 4. Abre la primera publicación y se verifica el detalle.
 * 5. Va a la sección de reseñas y le da like al primer comentario: el
 *    contador tiene que subir.
 * 6. Vuelve atrás, entra otra vez a la publicación, quita el like y el
 *    contador tiene que bajar.
 *
 * Es un solo @Test porque es un solo recorrido: los pasos dependen del
 * anterior (sin registro no hay sesión, y sin like no se puede quitar).
 * Requiere el emulador de Firebase corriendo.
 */
@RunWith(AndroidJUnit4::class)
class RegisterAndLikeReviewE2E {

    /**
     * Regla vacía y la Activity lanzada dentro del test: con
     * createAndroidComposeRule la Activity arranca ANTES de @Before y el
     * splash leería la sesión antes de que la prueba haya limpiado y
     * sembrado la base.
     */
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        runBlocking { E2ESeed.seedAll() }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking { E2ESeed.reset() }
    }

    @Test
    fun registroConPasswordCortaFallaYLuegoElUsuarioDaYQuitaLike() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // --- El usuario nuevo llega al registro ---
        composeRule.waitForTag("loginScreen")
        composeRule.onNodeWithText("Regístrate").performClick()
        composeRule.waitForTag("registerScreen")

        // --- Primer intento: la contraseña es muy corta ---
        composeRule.onNodeWithTag("registerFullName").performTextInput("Nuevo Usuario")
        composeRule.onNodeWithTag("registerEmail").performTextInput("nuevo@motormates.com")
        composeRule.onNodeWithTag("registerPassword").performTextInput("1234")
        composeRule.onNodeWithTag("registerConfirmPassword").performTextInput("1234")
        composeRule.onNodeWithTag("registerSubmit").performClick()

        // El mensaje no lo pone la pantalla: no hay validación de longitud en
        // el cliente. Lo arma AuthRepository.signUp al atrapar la
        // FirebaseAuthWeakPasswordException que devuelve Auth.
        composeRule.waitForTag("registerError")
        composeRule.onNodeWithTag("registerError")
            .assertTextEquals("La contraseña es muy débil, usa al menos 6 caracteres")

        // --- Segundo intento: corrige la contraseña y entra ---
        // Sin performTextClearance quedaría "1234123456" y el registro
        // pasaría por la razón equivocada.
        composeRule.onNodeWithTag("registerPassword").performTextClearance()
        composeRule.onNodeWithTag("registerPassword").performTextInput("123456")
        composeRule.onNodeWithTag("registerConfirmPassword").performTextClearance()
        composeRule.onNodeWithTag("registerConfirmPassword").performTextInput("123456")
        composeRule.onNodeWithTag("registerSubmit").performClick()

        composeRule.waitForTag("feedScreen")

        // --- La primera publicación y su detalle ---
        abrirPublicacion()
        verificarDetalle()

        // --- Sección de reseñas: like al primer comentario ---
        abrirSeccionDeResenias()
        composeRule.onNodeWithTag(contadorDeLikes).assertTextEquals("0")

        composeRule.onNodeWithTag(botonDeLike).performClick()
        composeRule.waitForTagText(contadorDeLikes, "1")
        composeRule.onNodeWithContentDescription("Quitar like").assertIsDisplayed()

        // --- Va atrás, vuelve a entrar y quita el like ---
        Espresso.pressBack() // de la lista de reseñas al detalle
        Espresso.pressBack() // del detalle al feed
        composeRule.waitForTag("feedScreen")

        abrirPublicacion()
        abrirSeccionDeResenias()
        composeRule.waitForTagText(contadorDeLikes, "1")

        composeRule.onNodeWithTag(botonDeLike).performClick()
        composeRule.waitForTagText(contadorDeLikes, "0")
        composeRule.onNodeWithContentDescription("Dar like").assertIsDisplayed()
    }

    private val botonDeLike = "likeButton_${E2ESeed.FIRST_REVIEW_ID}"
    private val contadorDeLikes = "likesCount_${E2ESeed.FIRST_REVIEW_ID}"
    private val tituloPublicacion = "${E2ESeed.BRAND} ${E2ESeed.MODEL}"

    /** Toca la tarjeta del feed; solo hay un vehículo sembrado. */
    private fun abrirPublicacion() {
        composeRule.waitForText(tituloPublicacion)
        composeRule.onNodeWithText(tituloPublicacion).performClick()
        composeRule.waitForTag("vehicleDetailScreen")
    }

    private fun verificarDetalle() {
        // La marca la pinta en mayúsculas el mapper (Vehicle.toCarDetailUi).
        composeRule.onNodeWithText(E2ESeed.BRAND.uppercase()).assertIsDisplayed()
        composeRule.onNodeWithText(E2ESeed.MODEL).assertIsDisplayed()
        composeRule.onNodeWithText("${E2ESeed.YEAR} · ${E2ESeed.CATEGORY}").assertIsDisplayed()
        composeRule.onNodeWithText(E2ESeed.POTENCIA).assertIsDisplayed()
        composeRule.onNodeWithText(E2ESeed.TRACCION).assertIsDisplayed()
        // La calificación y el número de reseñas se calculan con las reseñas
        // ya cargadas, que llegan por un listener aparte.
        composeRule.waitForText("${E2ESeed.REVIEWS_COUNT} reseñas", substring = true)
    }

    /** "Ver todas" aparece porque hay más reseñas de las que caben en línea. */
    private fun abrirSeccionDeResenias() {
        composeRule.waitForText("Ver todas")
        composeRule.onNodeWithText("Ver todas").performClick()
        composeRule.waitForTag(contadorDeLikes)
    }
}
