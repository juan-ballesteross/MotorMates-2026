package com.example.motormates.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Colores de marca (se mantienen, se siguen usando en varias pantallas) =====
val MotorMatesBackground = Color(0xFF0A0A0A)
val MotorMatesSurface = Color(0xFF1E1E1E)
val MotorMatesRed = Color(0xFFB3282D)
val MotorMatesRedLight = Color(0xFFE05257)
val MotorMatesTextSecondary = Color(0xFF9B9B9B)

/*
 * Colores de categoría de los marcadores del mapa.
 *
 * No son roles del tema y no cambian entre paleta clara y oscura: un marcador
 * se dibuja sobre el mapa, no sobre una superficie de la app, y lo que tiene
 * que hacer es distinguirse de las otras capas de un vistazo. La paleta de
 * marca es toda roja y ámbar (las reseñas usan el rojo primario y la
 * selección usa el ámbar terciario), así que hacen falta dos tonos que no se
 * confundan con ninguno de los dos.
 */
val MotorMatesCarMeetBlue = Color(0xFF3D7BD6)
val MotorMatesPiqueGreen = Color(0xFF36A85D)

/**
 * Las gasolineras. Un surtidor pide naranja, pero el ámbar terciario ya marca
 * la selección en el mapa y los dos se confundirían, así que queda el morado:
 * es el único tono que no choca con el rojo de las reseñas, el azul de los
 * Car Meets ni el verde de los piques. El significado lo carga el ícono.
 */
val MotorMatesFuelPurple = Color(0xFF8C5BD6)

/**
 * Los puntos de carga. Tiene que distinguirse del morado de las gasolineras
 * de un vistazo, porque las dos capas se pueden ver a la vez y los íconos
 * (surtidor y enchufe) son demasiado parecidos a tamaño de marcador.
 */
val MotorMatesChargingTeal = Color(0xFF14A6A6)

// ===== Paleta MD3 — DARK =====
val md3_dark_primary = MotorMatesRed
val md3_dark_onPrimary = Color(0xFFFFFFFF)
val md3_dark_primaryContainer = Color(0xFF7A1216)
val md3_dark_onPrimaryContainer = Color(0xFFFFDAD8)

val md3_dark_secondary = MotorMatesRedLight
val md3_dark_onSecondary = Color(0xFF3A0708)
val md3_dark_secondaryContainer = Color(0xFF8C2C2E)
val md3_dark_onSecondaryContainer = Color(0xFFFFDAD8)

val md3_dark_tertiary = Color(0xFFE0B84E)
val md3_dark_onTertiary = Color(0xFF3F2E00)
val md3_dark_tertiaryContainer = Color(0xFF594300)
val md3_dark_onTertiaryContainer = Color(0xFFFFE08A)

val md3_dark_error = Color(0xFFFFB4AB)
val md3_dark_onError = Color(0xFF690005)
val md3_dark_errorContainer = Color(0xFF93000A)
val md3_dark_onErrorContainer = Color(0xFFFFDAD6)

val md3_dark_background = MotorMatesBackground
val md3_dark_onBackground = Color(0xFFECE0DF)

val md3_dark_surface = MotorMatesSurface
val md3_dark_onSurface = Color(0xFFECE0DF)
val md3_dark_surfaceVariant = Color(0xFF534342)
val md3_dark_onSurfaceVariant = MotorMatesTextSecondary

val md3_dark_outline = Color(0xFF9F8C8A)
val md3_dark_outlineVariant = Color(0xFF534342)
val md3_dark_scrim = Color(0xFF000000)
val md3_dark_inverseSurface = Color(0xFFECE0DF)
val md3_dark_inverseOnSurface = Color(0xFF382E2D)
val md3_dark_inversePrimary = Color(0xFF9A4A45)

// ===== Paleta MD3 — LIGHT =====
val md3_light_primary = Color(0xFF9A4A45)
val md3_light_onPrimary = Color(0xFFFFFFFF)
val md3_light_primaryContainer = Color(0xFFFFDAD8)
val md3_light_onPrimaryContainer = Color(0xFF410002)

val md3_light_secondary = Color(0xFF77534F)
val md3_light_onSecondary = Color(0xFFFFFFFF)
val md3_light_secondaryContainer = Color(0xFFFFDAD8)
val md3_light_onSecondaryContainer = Color(0xFF2C1512)

val md3_light_tertiary = Color(0xFF6C5D2F)
val md3_light_onTertiary = Color(0xFFFFFFFF)
val md3_light_tertiaryContainer = Color(0xFFF6E1A7)
val md3_light_onTertiaryContainer = Color(0xFF221B00)

val md3_light_error = Color(0xFFBA1A1A)
val md3_light_onError = Color(0xFFFFFFFF)
val md3_light_errorContainer = Color(0xFFFFDAD6)
val md3_light_onErrorContainer = Color(0xFF410002)

val md3_light_background = Color(0xFFFFF8F7)
val md3_light_onBackground = Color(0xFF231918)

val md3_light_surface = Color(0xFFF6E8E6)
val md3_light_onSurface = Color(0xFF231918)
val md3_light_surfaceVariant = Color(0xFFF5DDDA)
val md3_light_onSurfaceVariant = Color(0xFF534342)

val md3_light_outline = Color(0xFF857370)
val md3_light_outlineVariant = Color(0xFFD8C2BE)
val md3_light_scrim = Color(0xFF000000)
val md3_light_inverseSurface = Color(0xFF392E2D)
val md3_light_inverseOnSurface = Color(0xFFFFEDEB)
val md3_light_inversePrimary = Color(0xFFFFB4AB)
