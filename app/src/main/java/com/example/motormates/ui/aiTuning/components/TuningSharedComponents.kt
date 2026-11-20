package com.example.motormates.ui.aiTuning.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.motormates.data.model.GroundedAnswer
import com.example.motormates.data.model.Part
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.model.TuningInsights
import com.example.motormates.data.model.TuningSummary
import com.example.motormates.data.util.CopFormat
import com.example.motormates.ui.theme.Radius
import java.util.Locale
import kotlin.math.roundToInt

/** Decodifica una sola vez por imagen (no en cada recomposición). */
@Composable
fun rememberTuningBitmap(image: TuningImage?): ImageBitmap? = remember(image) {
    image?.bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
}

/**
 * Comparador antes/después: la imagen "después" se recorta según la
 * posición del divisor, que se arrastra con el dedo (o se toca para
 * moverlo). Los dos lados se pintan con el mismo ContentScale.Crop en la
 * misma caja para que el carro quede alineado.
 */
@Composable
fun BeforeAfterSlider(
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 4f / 3f
) {
    var fraction by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("beforeAfterSlider")
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        Box(modifier = Modifier.fillMaxSize()) { before() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(left = size.width * fraction) { this@drawWithContent.drawContent() }
                }
        ) { after() }

        // Capa de gestos encima de todo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        fraction = (fraction + dragAmount / widthPx).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(widthPx) {
                    detectTapGestures { offset -> fraction = (offset.x / widthPx).coerceIn(0f, 1f) }
                }
        )

        // Divisor y manija.
        val dividerX = (widthPx * fraction).roundToInt()
        Box(
            modifier = Modifier
                .offset { IntOffset(dividerX - 1, 0) }
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(dividerX - 18.dp.roundToPx(), 0) }
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = "Arrastra para comparar", tint = Color.Black)
        }

        SliderLabel("ANTES", Modifier.align(Alignment.TopStart).padding(10.dp))
        SliderLabel("DESPUÉS", Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun SliderLabel(text: String, modifier: Modifier) {
    // Siempre sobre la foto: fondo negro semitransparente fijo, texto blanco.
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Imagen en bytes a pantalla completa de su caja, recortada. */
@Composable
fun TuningImageView(image: TuningImage?, modifier: Modifier = Modifier) {
    val bitmap = rememberTuningBitmap(image)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Vehículo modificado",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    }
}

// ---------------- especificaciones y costo ----------------

/** Tarjeta "antes → después" con potencia, 0-100, peso y altura. */
@Composable
fun SpecsComparisonCard(summary: TuningSummary, modifier: Modifier = Modifier) {
    TuningCard(title = "Rendimiento", modifier = modifier) {
        SpecRow(
            label = "Potencia",
            before = summary.before.hp?.let { "${it.roundToInt()} hp" },
            after = summary.after.hp?.let { "${it.roundToInt()} hp" },
            delta = summary.hpDeltaPct.takeIf { it != 0.0 }?.let { "${signed(it)} %" },
            improves = summary.hpDeltaPct > 0
        )
        SpecRow(
            label = "0-100 km/h *",
            before = summary.before.zeroTo100?.let { "${oneDecimal(it)} s" },
            after = summary.after.zeroTo100?.let { "${oneDecimal(it)} s" },
            delta = deltaOf(summary.before.zeroTo100, summary.after.zeroTo100)?.let { "${signed(it, 1)} s" },
            improves = (summary.after.zeroTo100 ?: 0.0) < (summary.before.zeroTo100 ?: 0.0)
        )
        SpecRow(
            label = "Peso *",
            before = summary.before.weightKg?.let { "${it.roundToInt()} kg" },
            after = summary.after.weightKg?.let { "${it.roundToInt()} kg" },
            delta = summary.weightDeltaKg.takeIf { it != 0.0 }?.let { "${signed(it)} kg" },
            improves = summary.weightDeltaKg < 0
        )
        if (summary.torqueDeltaPct != 0.0) {
            SpecRow(
                label = "Torque",
                before = null,
                after = null,
                delta = "${signed(summary.torqueDeltaPct)} %",
                improves = summary.torqueDeltaPct > 0
            )
        }
        if (summary.heightDeltaMm != 0) {
            SpecRow(
                label = "Altura",
                before = null,
                after = null,
                delta = "${signed(summary.heightDeltaMm.toDouble())} mm",
                improves = true
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append("* Estimados: el peso base es típico de la categoría y el 0-100 se ")
                append("calcula con la relación peso/potencia.")
                if (summary.hpGainCapped) append(" La ganancia de potencia se limitó al tope de 35 %.")
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SpecRow(label: String, before: String?, after: String?, delta: String?, improves: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (before != null && after != null) {
            Text(before, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("  →  ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(after, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (delta != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = delta,
                color = if (improves) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Desglose de precio por pieza + total. */
@Composable
fun CostBreakdownCard(
    parts: List<Part>,
    summary: TuningSummary,
    modifier: Modifier = Modifier,
    trailing: (@Composable (Part) -> Unit)? = null
) {
    TuningCard(title = "Costo estimado", modifier = modifier) {
        parts.forEach { part ->
            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(part.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, maxLines = 2)
                        Text(part.brand, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                    }
                    Text(
                        CopFormat.range(part.priceMinCop, part.priceMaxCop),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                trailing?.invoke(part)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Total", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                CopFormat.range(summary.totalMinCop, summary.totalMaxCop),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        if (summary.hasEstimatedPrices) {
            Text(
                "Rangos de referencia del catálogo; verifica el precio con la tienda o el taller.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Ficha técnica redactada por la AI. */
@Composable
fun InsightsCard(insights: TuningInsights, modifier: Modifier = Modifier) {
    TuningCard(title = "Ficha técnica AI", icon = { Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary) }, modifier = modifier) {
        if (insights.summary.isNotBlank()) {
            Text(insights.summary, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }
        InsightLine("Manejo", insights.handling)
        InsightLine("Consumo", insights.consumption)
        InsightLine("Garantía y desgaste", insights.warranty)
        if (insights.tips.isNotEmpty()) InsightLine("Recomendaciones", insights.tips.joinToString("\n") { "• $it" })
        if (insights.legal.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Gavel, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Legal en Colombia", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            insights.legal.forEach {
                Text("• $it", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            "Generado por AI a partir de los cálculos de la app. Es orientativo, no reemplaza a un mecánico.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun InsightLine(title: String, body: String) {
    if (body.isBlank()) return
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

/**
 * Respuesta con búsqueda de Google. Los términos de uso del grounding
 * obligan a mostrar las fuentes y las sugerencias de búsqueda
 * (searchEntryPoint) tal como vienen: por eso el HTML va en un WebView.
 */
@Composable
fun GroundedAnswerView(answer: GroundedAnswer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(answer.text.ifBlank { "Sin respuesta" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        if (answer.sources.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Fuentes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            answer.sources.take(5).forEach { source ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.openUrl(source.uri) }
                        .padding(vertical = 3.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        source.title,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        answer.searchEntryPointHtml?.let { html ->
            Spacer(Modifier.height(6.dp))
            SearchSuggestionsView(html)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SearchSuggestionsView(html: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                // Los chips de sugerencias son enlaces a Google: se abren fuera de la app.
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        view.context.openUrl(request.url.toString())
                        return true
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    )
}

/** Contenedor común de las tarjetas de resultado. */
@Composable
fun TuningCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Radius.lg))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            icon?.invoke()
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

internal fun android.content.Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun deltaOf(before: Double?, after: Double?): Double? =
    if (before != null && after != null && before != after) after - before else null

internal fun signed(value: Double, decimals: Int = 0): String {
    val pattern = if (decimals == 0 && value % 1.0 == 0.0) "%.0f" else "%.${decimals.coerceAtLeast(1)}f"
    val text = String.format(Locale.ROOT, pattern, value)
    return if (value > 0) "+$text" else text
}

private fun oneDecimal(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

