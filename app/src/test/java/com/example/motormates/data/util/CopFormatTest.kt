package com.example.motormates.data.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CopFormatTest {

    @Test
    fun `full usa punto de miles`() {
        assertThat(CopFormat.full(1_800_000)).isEqualTo("$1.800.000")
        assertThat(CopFormat.full(950)).isEqualTo("$950")
        assertThat(CopFormat.full(0)).isEqualTo("$0")
    }

    @Test
    fun `compact abrevia millones con coma decimal`() {
        assertThat(CopFormat.compact(1_800_000)).isEqualTo("$1,8 M")
        assertThat(CopFormat.compact(5_000_000)).isEqualTo("$5 M")
        assertThat(CopFormat.compact(12_350_000)).isEqualTo("$12,4 M")
        assertThat(CopFormat.compact(150_000_000)).isEqualTo("$150 M")
    }

    @Test
    fun `compact abrevia miles y deja lo pequeno igual`() {
        assertThat(CopFormat.compact(850_000)).isEqualTo("$850 mil")
        assertThat(CopFormat.compact(300)).isEqualTo("$300")
    }

    @Test
    fun `compact respeta el signo`() {
        assertThat(CopFormat.compact(-2_500_000)).isEqualTo("-$2,5 M")
    }

    @Test
    fun `range une minimo y maximo, o muestra uno si son iguales`() {
        assertThat(CopFormat.range(1_800_000, 2_400_000)).isEqualTo("$1,8 M – $2,4 M")
        assertThat(CopFormat.range(500_000, 500_000)).isEqualTo("$500 mil")
    }
}
