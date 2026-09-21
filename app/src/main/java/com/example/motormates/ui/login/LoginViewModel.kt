package com.example.motormates.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
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
}
