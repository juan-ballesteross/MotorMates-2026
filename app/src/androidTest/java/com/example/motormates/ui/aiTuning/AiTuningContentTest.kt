package com.example.motormates.ui.aiTuning

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.motormates.data.model.PartCategory
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.Vehicle
import com.example.motormates.data.source.PartsCatalogParser
import com.example.motormates.data.util.CopFormat
import com.example.motormates.data.util.SpecCalculator
import com.example.motormates.data.util.SpecParser
import com.example.motormates.ui.theme.MotorMatesTheme
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pinta AiTuningContent con estados armados a mano (sin ViewModel, sin
 * Firebase ni AI) usando el catálogo REAL de assets. Verifica que el
 * configurador y el resultado muestren lo que el estado dice.
 */
@RunWith(AndroidJUnit4::class)
class AiTuningContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val porsche = Vehicle(
        id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
        imageUrl = "https://example.com/911.jpg", potencia = "502 hp", aceleracion = "3.4 s",
        velocidadMaxima = "318 km/h", traccion = "RWD"
    )

    private val catalog = InstrumentationRegistry.getInstrumentation().targetContext.assets
        .open("parts_catalog.json").bufferedReader().use { PartsCatalogParser.parse(it.readText()) }
        .compatibleWith(porsche.category)

    private fun configuringState(selection: Map<PartCategory, String> = emptyMap()): AiTuningState {
        val selected = selection.values.map { id -> catalog.parts.first { it.id == id } }
        return AiTuningState(
            isLoading = false,
            vehicle = porsche,
            parts = catalog.parts,
            packages = catalog.packages,
            selectedCategory = PartCategory.RINES,
            selection = selection,
            summary = SpecCalculator.calculate(SpecParser.specsOf(porsche), selected),
            remainingGenerations = 5
        )
    }

    private fun setContent(state: AiTuningState, actions: AiTuningActions = AiTuningActions()) {
        composeRule.setContent {
            MotorMatesTheme {
                Box { AiTuningContent(state = state, actions = actions, snackbarHostState = SnackbarHostState()) }
            }
        }
    }

    @Test
    fun configurador_muestra_paquetes_categorias_y_piezas_compatibles() {
        setContent(configuringState())

        composeRule.onNodeWithTag("package_street").assertIsDisplayed()
        composeRule.onNodeWithTag("category_RINES").assertIsDisplayed()
        composeRule.onNodeWithTag("part_rin_18_5radios_negro").assertIsDisplayed()
        // El rin off-road es solo para SUV/Pickup: no se ofrece a un deportivo.
        composeRule.onAllNodesWithTag("part_rin_17_offroad_negro").assertCountEquals(0)
        // Sin piezas no se puede generar.
        composeRule.onNodeWithTag("generateButton").assertIsNotEnabled()
        composeRule.onNodeWithText("Te quedan 5 imágenes hoy").assertIsDisplayed()
    }

    @Test
    fun configurador_con_seleccion_muestra_total_y_habilita_generar() {
        val state = configuringState(
            mapOf(PartCategory.RINES to "rin_18_5radios_negro", PartCategory.ECU to "ecu_stage1")
        )
        setContent(state)

        composeRule.onNodeWithTag("selectionTotal").assertIsDisplayed()
        composeRule.onNodeWithText(CopFormat.range(state.summary.totalMinCop, state.summary.totalMaxCop))
            .assertIsDisplayed()
        composeRule.onNodeWithText("2 piezas").assertIsDisplayed()
        composeRule.onNodeWithTag("generateButton").assertIsEnabled()
    }

    @Test
    fun tocar_pieza_y_paquete_llama_a_las_acciones() {
        var toggled: String? = null
        var pkg: String? = null
        setContent(
            configuringState(),
            AiTuningActions(onTogglePart = { toggled = it }, onApplyPackage = { pkg = it })
        )

        composeRule.onNodeWithTag("part_rin_18_5radios_negro").performClick()
        composeRule.onNodeWithTag("package_street").performClick()

        assertThat(toggled).isEqualTo("rin_18_5radios_negro")
        assertThat(pkg).isEqualTo("street")
    }

    @Test
    fun resultado_muestra_comparador_rendimiento_costo_y_ficha() {
        val parts = listOf("rin_18_5radios_negro", "ecu_stage1").map { id -> catalog.parts.first { it.id == id } }
        val summary = SpecCalculator.calculate(SpecParser.specsOf(porsche), parts)
        val state = configuringState().copy(
            phase = TuningPhase.RESULT,
            resultImage = fakeImage(),
            resultParts = parts,
            resultSummary = summary,
            insights = TuningInsights(summary = "Build listo para pista", legal = listOf("Revisar ruido del escape"))
        )
        setContent(state)

        composeRule.onNodeWithTag("beforeAfterSlider").assertIsDisplayed()
        composeRule.onNodeWithText("ANTES").assertIsDisplayed()
        composeRule.onNodeWithText("DESPUÉS").assertIsDisplayed()
        composeRule.onNodeWithTag("refineField").assertIsDisplayed()

        // 502 hp + 15 % = 577 hp.
        scrollTo(hasText("577 hp"))
        composeRule.onNodeWithText("577 hp").assertIsDisplayed()
        scrollTo(hasText(CopFormat.range(summary.totalMinCop, summary.totalMaxCop)))
        scrollTo(hasText("Build listo para pista"))
        composeRule.onNodeWithText("Build listo para pista").assertIsDisplayed()
        scrollTo(hasTestTag("publishBuildButton"))
        composeRule.onNodeWithTag("publishBuildButton").assertIsDisplayed()
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(matcher)
    }

    private fun fakeImage(): TuningImage {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return TuningImage(out.toByteArray())
    }
}
