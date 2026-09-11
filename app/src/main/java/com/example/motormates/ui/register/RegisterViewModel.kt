package com.example.motormates.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motormates.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun updateFullName(input: String) {
        _uiState.update { it.copy(fullName = input, errorMessage = null) }
    }

    fun updateEmail(input: String) {
        _uiState.update { it.copy(email = input, errorMessage = null) }
    }

    fun updatePassword(input: String) {
        _uiState.update { it.copy(password = input, errorMessage = null) }
    }

    fun updateConfirmPassword(input: String) {
        _uiState.update { it.copy(confirmPassword = input, errorMessage = null) }
    }

    fun updateTermsAccepted(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun signUp() {
        val currentState = _uiState.value
        if (currentState.isLoading ||
            currentState.fullName.isBlank() ||
            currentState.email.isBlank() ||
            currentState.password.isBlank() ||
            currentState.password != currentState.confirmPassword ||
            !currentState.termsAccepted
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isRegistered = false
                )
            }

            val result = authRepository.signUp(
                currentState.fullName,
                currentState.email,
                currentState.password
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        isRegistered = true
                    )
                } else {
                    val message = when (result.exceptionOrNull()) {
                        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
                        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil"
                        is FirebaseAuthInvalidCredentialsException -> "El correo no es válido"
                        else -> "No se pudo crear la cuenta"
                    }
                    it.copy(
                        isLoading = false,
                        errorMessage = message,
                        isRegistered = false
                    )
                }
            }
        }
    }
}
