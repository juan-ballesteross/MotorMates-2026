package com.example.motormates.data.util

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Test

/**
 * La combinación de fecha y hora de los eventos.
 *
 * Es la prueba más valiosa de la función, porque el error es silencioso: el
 * calendario de Material 3 devuelve medianoche UTC y el reloj devuelve hora
 * local, así que sumar millis guarda el evento corrido por el desfase del
 * dispositivo y nadie se da cuenta hasta que alguien llega a otra hora.
 *
 * La zona entra por parámetro, así que no hace falta Robolectric.
 */
class EventDateTimeTest {

    private val bogota = ZoneId.of("America/Bogota") // UTC-5, sin horario de verano
    private val madrid = ZoneId.of("Europe/Madrid")

    /** 20 de noviembre de 2025, medianoche UTC: lo que da el calendario. */
    private val dia = LocalDate.of(2025, 11, 20)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `la fecha del calendario y la hora del reloj se combinan en la zona dada`() {
        // Act: las 7 de la noche del 20 de noviembre en Bogotá.
        val millis = combineToEpochMillis(dateUtcMillis = dia, hour = 19, minute = 0, zone = bogota)

        // Assert: visto desde Bogotá, son las 19:00 de ese mismo día.
        val enBogota = Instant.ofEpochMilli(millis).atZone(bogota)
        assertThat(enBogota.hour).isEqualTo(19)
        assertThat(enBogota.minute).isEqualTo(0)
        assertThat(enBogota.toLocalDate()).isEqualTo(LocalDate.of(2025, 11, 20))
    }

    /**
     * El error concreto que esta función evita: sumar los millis de la hora a
     * la medianoche UTC. En Bogotá (UTC-5) eso daría las 2 p. m.
     */
    @Test
    fun `no es lo mismo que sumar los millis de la hora a la medianoche UTC`() {
        val correcto = combineToEpochMillis(dia, hour = 19, minute = 0, zone = bogota)
        val ingenuo = dia + 19 * 60 * 60 * 1000L

        assertThat(correcto).isNotEqualTo(ingenuo)
        // Cinco horas de diferencia, que es justo el desfase de Bogotá.
        assertThat(correcto - ingenuo).isEqualTo(5 * 60 * 60 * 1000L)
        assertThat(Instant.ofEpochMilli(ingenuo).atZone(bogota).hour).isEqualTo(14)
    }

    @Test
    fun `la misma fecha y hora en otra zona dan un instante distinto`() {
        val enBogota = combineToEpochMillis(dia, hour = 19, minute = 0, zone = bogota)
        val enMadrid = combineToEpochMillis(dia, hour = 19, minute = 0, zone = madrid)

        assertThat(enBogota).isNotEqualTo(enMadrid)
    }

    @Test
    fun `los minutos tambien se respetan`() {
        val millis = combineToEpochMillis(dia, hour = 9, minute = 30, zone = bogota)

        val enBogota = Instant.ofEpochMilli(millis).atZone(bogota)
        assertThat(enBogota.hour).isEqualTo(9)
        assertThat(enBogota.minute).isEqualTo(30)
    }

    /**
     * Ida y vuelta: lo que se guardó tiene que volver al calendario como el
     * mismo día, o al editar un evento aparecería el día anterior.
     */
    @Test
    fun `volver al calendario devuelve el mismo dia`() {
        val millis = combineToEpochMillis(dia, hour = 23, minute = 45, zone = bogota)

        assertThat(millis.toDatePickerMillis(bogota)).isEqualTo(dia)
    }

    @Test
    fun `volver al reloj devuelve la misma hora local`() {
        val millis = combineToEpochMillis(dia, hour = 23, minute = 45, zone = bogota)

        assertThat(millis.toLocalHourMinute(bogota)).isEqualTo(23 to 45)
    }

    /**
     * El caso que delata el error de zona: a las 23:45 en Bogotá ya es el día
     * siguiente en UTC, así que una conversión descuidada mostraría el 21.
     */
    @Test
    fun `una hora nocturna no adelanta el dia al volver al calendario`() {
        val millis = combineToEpochMillis(dia, hour = 23, minute = 45, zone = bogota)

        val enUtc = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        assertThat(enUtc).isEqualTo(LocalDate.of(2025, 11, 21)) // en UTC ya es el 21
        // Y aun así, el calendario tiene que seguir mostrando el 20.
        assertThat(millis.toDatePickerMillis(bogota)).isEqualTo(dia)
    }

    // ---------- Formato ----------

    @Test
    fun `hoy y maniana se dicen con palabras`() {
        val hoy = LocalDate.of(2025, 11, 20)
        val inicio = combineToEpochMillis(dia, hour = 19, minute = 0, zone = bogota)
        val maniana = combineToEpochMillis(
            LocalDate.of(2025, 11, 21).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            hour = 19,
            minute = 0,
            zone = bogota
        )

        assertThat(inicio.toEventDateTimeEs(zone = bogota, today = hoy)).startsWith("hoy, ")
        assertThat(maniana.toEventDateTimeEs(zone = bogota, today = hoy)).startsWith("mañana, ")
    }

    @Test
    fun `una fecha lejana se dice con el dia y el mes`() {
        val hoy = LocalDate.of(2025, 11, 1)
        val millis = combineToEpochMillis(dia, hour = 19, minute = 0, zone = bogota)

        val texto = millis.toEventDateTimeEs(zone = bogota, today = hoy)

        assertThat(texto).doesNotContain("hoy")
        assertThat(texto).doesNotContain("mañana")
        assertThat(texto).contains("20")
    }
}
