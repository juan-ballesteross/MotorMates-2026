package com.example.motormates.ui.register

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = true
)
