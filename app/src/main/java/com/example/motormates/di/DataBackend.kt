package com.example.motormates.di

/** De dónde salen los datos de la app (Auth y las imágenes siguen siempre en Firebase). */
enum class DataBackend { FIREBASE, EXPRESS }

/**
 * La única línea que hay que cambiar para pasar de Firestore al backend
 * propio (Express + Postgres) o al revés. DataSourceModule lee este valor y
 * le entrega a los repositorios la implementación que corresponde; ni los
 * repositorios, ni los ViewModels, ni la UI se enteran del cambio.
 */

// DataBackend.EXPRESS o DataBackend.FIREBASE
    val ACTIVE_BACKEND = DataBackend.FIREBASE