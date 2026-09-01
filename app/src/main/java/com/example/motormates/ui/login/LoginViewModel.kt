package com.example.motormates.ui.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Un MutableStateFlow privado + StateFlow público por cada campo,
 * igual que en CommentsViewModel. La pantalla nunca modifica estos
 * valores directamente, solo llama a las funciones de abajo.
 */
class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible

    fun updateEmail(input: String) {
        _email.value = input
    }

    fun updatePassword(input: String) {
        _password.value = input
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }
}