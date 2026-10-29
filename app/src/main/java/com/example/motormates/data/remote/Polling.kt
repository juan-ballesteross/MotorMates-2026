package com.example.motormates.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

internal const val POLLING_INTERVAL_MS = 5_000L

/**
 * El backend propio no empuja cambios como un listener de Firestore, así
 * que el "tiempo real" se imita preguntando cada pocos segundos mientras
 * alguien recolecta el flow. distinctUntilChanged evita repintar la UI
 * cuando la respuesta no cambió. Al cancelarse la recolección (la pantalla
 * se va) el loop se detiene solo, igual que el awaitClose de un listener.
 * Un error corta el flow, igual que el close(error) de los listeners: el
 * repositorio lo convierte en Result.failure.
 */
fun <T> pollingFlow(intervalMs: Long = POLLING_INTERVAL_MS, fetch: suspend () -> T): Flow<T> =
    flow {
        while (true) {
            emit(fetch())
            delay(intervalMs)
        }
    }.distinctUntilChanged()
