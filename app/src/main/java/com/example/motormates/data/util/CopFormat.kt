package com.example.motormates.data.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Formato de pesos colombianos: punto de miles y coma decimal, como se
 * escribe en Colombia ("$1.800.000", "$1,8 M").
 */
object CopFormat {

    private val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    /** "$1.800.000" */
    fun full(value: Long): String = "$" + DecimalFormat("#,##0", symbols).format(value)

    /** "$1,8 M" para millones, "$850 mil" para miles, "$500" para lo demás. */
    fun compact(value: Long): String {
        val absolute = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            // Aritmética entera con redondeo hacia arriba: con Double, 12,35 M
            // queda en 12,349999… y saldría "12,3".
            absolute >= 100_000_000 -> "$sign$${(absolute + 500_000) / 1_000_000} M"
            absolute >= 1_000_000 -> {
                val tenths = (absolute + 50_000) / 100_000
                val text = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
                "$sign$$text M"
            }
            absolute >= 1_000 -> "$sign$${(absolute + 500) / 1_000} mil"
            else -> "$sign$$absolute"
        }
    }

    /** "$1,8 M – $2,4 M"; si mínimo y máximo coinciden, un solo valor. */
    fun range(min: Long, max: Long): String =
        if (min == max) compact(min) else "${compact(min)} – ${compact(max)}"
}
