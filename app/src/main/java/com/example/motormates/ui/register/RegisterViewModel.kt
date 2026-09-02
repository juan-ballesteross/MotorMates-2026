package com.example.motormates.ui.register

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Un MutableStateFlow privado + StateFlow público por cada campo,
 * mismo patrón que LoginViewModel. La pantalla nunca modifica estos
 * valores directamente, solo llama a las funciones de abajo.
 */
class RegisterViewModel : ViewModel() {

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword

    private val _termsAccepted = MutableStateFlow(true)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted

    fun updateFullName(input: String) {
        _fullName.value = input
    }

    fun updateEmail(input: String) {
        _email.value = input
    }

    fun updatePassword(input: String) {
        _password.value = input
    }

    fun updateConfirmPassword(input: String) {
        _confirmPassword.value = input
    }

    fun updateTermsAccepted(accepted: Boolean) {
        _termsAccepted.value = accepted
    }
}
