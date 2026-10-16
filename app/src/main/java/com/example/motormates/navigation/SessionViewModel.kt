package com.example.motormates.navigation

import androidx.lifecycle.ViewModel
import com.example.motormates.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Le da al grafo de navegación el uid de la sesión actual, para decidir si
 * tocar un autor abre "mi perfil" o el perfil público. Se consulta en el
 * momento del clic (no se guarda) porque la sesión cambia al hacer login.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun currentUserId(): String? = authRepository.getCurrentUser()?.id
}
