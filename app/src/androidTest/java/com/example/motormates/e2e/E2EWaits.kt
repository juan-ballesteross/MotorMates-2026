package com.example.motormates.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText

/**
 * Esperas de las pruebas E2E.
 *
 * Cada paso del flujo pasa por Firestore, así que el assert se ejecutaría
 * antes de que llegue la respuesta (la condición de carrera que describe la
 * guía en §9). El timeout por defecto de waitUntil es de 1 segundo, muy poco
 * para una ida y vuelta al emulador: aquí se usan 15 s.
 *
 * Sobre useUnmergedTree: Modifier.clickable fusiona la semántica de sus
 * hijos (mergeDescendants), así que un testTag puesto DENTRO de algo
 * clicable —el contador de likes dentro de su fila, el número de seguidores
 * dentro del StatItem— no existe en el árbol fusionado, que es el que
 * consultan los matchers por defecto. Para esos hay que pedir el árbol sin
 * fusionar.
 */
const val E2E_TIMEOUT_MS = 15_000L

/** Espera a que exista un nodo con ese testTag. */
fun ComposeTestRule.waitForTag(
    tag: String,
    useUnmergedTree: Boolean = false,
    timeoutMillis: Long = E2E_TIMEOUT_MS
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
    }
}

/** Espera a que ese texto aparezca en pantalla. */
fun ComposeTestRule.waitForText(
    text: String,
    substring: Boolean = false,
    timeoutMillis: Long = E2E_TIMEOUT_MS
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Espera a que el nodo con ese tag muestre exactamente ese texto. Es la que
 * usan los contadores: el corazón y el botón de seguir son optimistas (el
 * ViewModel los cambia de inmediato), pero el número llega después, cuando
 * el listener de Firestore reemite el documento.
 */
fun ComposeTestRule.waitForTagText(
    tag: String,
    text: String,
    useUnmergedTree: Boolean = false,
    timeoutMillis: Long = E2E_TIMEOUT_MS
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().any { node ->
            node.config.getOrNull(SemanticsProperties.Text)
                ?.any { it.text == text } == true
        }
    }
}
