package com.example.motormates.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Abre un punto en la app de mapas que el usuario prefiera.
 *
 * Se usa el esquema `geo:`, que es el genérico de Android, y no una URL de
 * Google Maps: así el sistema ofrece todas las apps instaladas (Maps, Waze, la
 * que sea) en vez de imponer una. MotorMates no dibuja la ruta ni consulta
 * ninguna API de rutas — le pasa el punto a quien sabe hacerlo.
 *
 * El formato es `geo:0,0?q=lat,lng(etiqueta)`: el `0,0` del principio es un
 * centro que se ignora cuando viene `q`, y la etiqueta es lo que la otra app
 * muestra en el pin.
 *
 * @return false si no hay ninguna app que atienda `geo:`, para que la pantalla
 *   pueda decírselo al usuario.
 */
fun openPointInMapsApp(context: Context, lat: Double, lng: Double, label: String): Boolean {
    // La etiqueta se codifica: los nombres de los lugares traen espacios,
    // comas y acentos, y sin codificar romperían la URI.
    val uri = Uri.parse("geo:0,0?q=$lat,$lng(${Uri.encode(label)})")

    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: ActivityNotFoundException) {
        // Se comprueba atrapando y no con resolveActivity() a propósito: desde
        // Android 11 ese método devuelve null si la app no declara un bloque
        // <queries> en el manifest, así que diría que no hay ninguna app de
        // mapas incluso en un teléfono que tiene Google Maps instalado.
        false
    }
}
