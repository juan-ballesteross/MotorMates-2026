package com.example.motormates.data.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fecha y hora de los eventos.
 *
 * La zona entra por parámetro en todas las funciones, con la del dispositivo
 * por defecto: así se pueden probar en la JVM sin Robolectric, fijando una
 * zona conocida. minSdk 26 garantiza java.time sin desugaring.
 */

private val ZONA_CO: Locale = Locale.forLanguageTag("es-CO")

/**
 * Junta el día que devuelve el calendario de Material 3 con la hora que
 * devuelve el reloj.
 *
 * Es el cálculo más fácil de equivocar de toda la función: [dateUtcMillis]
 * es la medianoche **UTC** del día elegido (así lo entrega
 * DatePickerState.selectedDateMillis), mientras que [hour] y [minute] son
 * hora de **reloj de pared local**. Sumar millis
 * (`dateUtcMillis + hour * 3_600_000`) corre el instante por el desfase del
 * dispositivo: en Colombia, un evento puesto a las 19:00 quedaría guardado
 * como 19:00 UTC y se mostraría a las 2:00 p. m.
 *
 * Lo correcto es reconstruir la fecha local y convertirla UNA vez con la zona
 * del teléfono, que es la que el usuario tiene en la cabeza.
 */
internal fun combineToEpochMillis(
    dateUtcMillis: Long,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault()
): Long = Instant.ofEpochMilli(dateUtcMillis)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .atTime(hour, minute)
    .atZone(zone)
    .toInstant()
    .toEpochMilli()

/** El día (en UTC, como lo quiere el calendario) de un instante local. */
internal fun Long.toDatePickerMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(this)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

/** La hora y el minuto locales de un instante, para precargar el reloj. */
internal fun Long.toLocalHourMinute(zone: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
    val time = Instant.ofEpochMilli(this).atZone(zone)
    return time.hour to time.minute
}

/**
 * "jue 20 nov, 7:00 p. m.", o "hoy, 7:00 p. m." y "mañana, 7:00 p. m." cuando
 * aplica: un evento se consulta sobre todo para saber si es hoy.
 */
fun Long.toEventDateTimeEs(
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone)
): String {
    val fecha = Instant.ofEpochMilli(this).atZone(zone)
    val hora = fecha.format(DateTimeFormatter.ofPattern("h:mm a", ZONA_CO))
    val dia = when (fecha.toLocalDate()) {
        today -> "hoy"
        today.plusDays(1) -> "mañana"
        else -> fecha.format(DateTimeFormatter.ofPattern("EEE d MMM", ZONA_CO))
    }
    return "$dia, $hora"
}

/** Solo la hora, para la línea "de 7:00 p. m. a 11:00 p. m." */
fun Long.toEventTimeEs(zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this)
        .atZone(zone)
        .format(DateTimeFormatter.ofPattern("h:mm a", ZONA_CO))
