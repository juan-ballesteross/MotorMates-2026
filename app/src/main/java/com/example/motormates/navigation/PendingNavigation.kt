package com.example.motormates.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A dónde quiere ir el usuario al tocar una notificación (p. ej. el
 * resultado de AI Tuning). MainActivity lo recibe en el Intent y
 * AppNavigation lo consume cuando ya hay sesión (después del splash).
 */
@Singleton
class PendingNavigation @Inject constructor() {

    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route.asStateFlow()

    fun offer(route: String?) {
        if (!route.isNullOrBlank()) _route.value = route
    }

    /** Devuelve la ruta pendiente (si hay) y la borra para no navegar dos veces. */
    fun consume(): String? = _route.value.also { _route.value = null }

    companion object {
        /** Extra del Intent de la notificación con la ruta de destino. */
        const val EXTRA_ROUTE = "com.example.motormates.EXTRA_ROUTE"
    }
}
