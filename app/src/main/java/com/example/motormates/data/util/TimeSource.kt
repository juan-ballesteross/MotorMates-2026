package com.example.motormates.data.util

import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * La hora actual y un "tic" periódico, inyectables.
 *
 * Existe para que las pantallas que dependen del reloj (el mapa muestra solo
 * lo de las últimas 24 horas) se puedan probar sin esperar de verdad: en las
 * pruebas se reemplaza por un mock que fija la hora y controla los tics. Un
 * while(true) { delay() } directo en el ViewModel colgaría advanceUntilIdle().
 */
interface TimeSource {
    fun nowMillis(): Long

    /** Emite cada periodMillis mientras alguien recolecte. No emite al empezar. */
    fun ticks(periodMillis: Long): Flow<Unit>
}

class SystemTimeSource @Inject constructor() : TimeSource {

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun ticks(periodMillis: Long): Flow<Unit> = flow {
        while (true) {
            delay(periodMillis)
            emit(Unit)
        }
    }
}
