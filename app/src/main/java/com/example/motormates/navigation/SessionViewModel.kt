package com.example.motormates.navigation

import androidx.lifecycle.ViewModel
import com.example.motormates.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Le da al grafo de navegación el uid de la sesión actual, para decidir si
 * tocar un autor abre "mi perfil" o el perfil público. Se consulta en el
 * momento del clic (no se guarda) porque la sesión cambia al hacer login.
 *
 * También expone la ruta que dejó pendiente una notificación tocada.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pendingNavigation: PendingNavigation
) : ViewModel() {

    val pendingRoute: StateFlow<String?> = pendingNavigation.route

    fun currentUserId(): String? = authRepository.getCurrentUser()?.id

    fun consumePendingRoute(): String? = pendingNavigation.consume()
}
