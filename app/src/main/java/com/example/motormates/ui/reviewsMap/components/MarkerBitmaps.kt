package com.example.motormates.ui.reviewsMap.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Descarga las fotos de los marcadores como bitmaps antes de dibujarlos.
 *
 * No sirve poner un AsyncImage dentro del marcador: el contenido de un
 * marcador de Compose se convierte a bitmap una sola vez, al momento, y la
 * foto todavía no habría llegado. Por eso se precargan con el ImageLoader de
 * Coil (comparte su caché de memoria y disco con el resto de la app, así que
 * volver a la pestaña no descarga nada) y el marcador se redibuja cuando
 * aparece su bitmap.
 *
 * Se piden solo las URL distintas: el catálogo tiene pocos carros aunque
 * haya muchas reseñas.
 *
 * @return url -> bitmap, con lo que se haya podido descargar hasta ahora.
 */
@Composable
fun rememberMarkerBitmaps(urls: Set<String>, sizePx: Int): Map<String, ImageBitmap> {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    LaunchedEffect(urls) {
        val missing = urls - bitmaps.keys
        if (missing.isEmpty()) return@LaunchedEffect

        val loaded = coroutineScope {
            missing.map { url -> async { url to loadBitmap(context, url, sizePx) } }.awaitAll()
        }
        bitmaps = bitmaps + loaded.mapNotNull { (url, bitmap) -> bitmap?.let { url to it } }
    }

    return bitmaps
}

/**
 * allowHardware(false) es obligatorio: los bitmaps de hardware no se pueden
 * dibujar en el Canvas por software con el que se rasteriza el marcador.
 * Una foto que falla devuelve null y el marcador queda con el ícono.
 */
private suspend fun loadBitmap(context: Context, url: String, sizePx: Int): ImageBitmap? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(sizePx)
        .scale(Scale.FILL)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    return runCatching { result.drawable.toBitmap().asImageBitmap() }.getOrNull()
}
