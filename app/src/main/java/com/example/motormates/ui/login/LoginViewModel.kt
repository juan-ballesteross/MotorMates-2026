package com.example.motormates.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.AuthRepository
import com.example.motormates.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun updateEmail(input: String) {
        _uiState.update { it.copy(email = input, errorMessage = null) }
    }

    fun updatePassword(input: String) {
        _uiState.update { it.copy(password = input, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun signIn() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.email.isBlank() || currentState.password.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isLoggedIn = false
                )
            }

            val result = authRepository.signIn(currentState.email, currentState.password)

            // La cuenta puede autenticarse y no tener perfil en esta base de
            // datos (cuentas de versiones anteriores de la app). Si se entrara
            // igual, el Splash cerraría la sesión en el siguiente arranque y
            // el usuario quedaría dando vueltas.
            // getOrDefault(true): si no se pudo comprobar (sin red) se deja
            // pasar, mismo criterio que el Splash.
            val user = result.getOrNull()
            if (user != null && !userRepository.hasProfile(user.id).getOrDefault(true)) {
                authRepository.signOut()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ACCOUNT_WITHOUT_PROFILE,
                        isLoggedIn = false
                    )
                }
                return@launch
            }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        isLoggedIn = true
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        // Antes: mensaje fijo que ignoraba el error real.
                        // Ahora: usa el mensaje específico que armó el
                        // Repository (credenciales, red, etc.), con un
                        // genérico de respaldo solo por si acaso.
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "Error al iniciar sesión",
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    private companion object {
        const val ACCOUNT_WITHOUT_PROFILE =
            "Esta cuenta es de una versión anterior de la app y ya no tiene perfil. " +
                "Regístrate con otro correo."
    }
}
