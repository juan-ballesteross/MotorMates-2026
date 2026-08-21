package com.example.motormates.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Pantalla de registro. El botón "Crear cuenta" solo se habilita cuando el
 * formulario es válido (campos completos, contraseñas coincidentes y
 * términos aceptados).
 */
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(true) }

    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
    val isFormValid = fullName.isNotBlank() &&
        email.isNotBlank() &&
        password.isNotBlank() &&
        password == confirmPassword &&
        termsAccepted

    RegisterScreenContent(
        fullName = fullName,
        onFullNameChange = { fullName = it },
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        passwordsMatch = passwordsMatch,
        termsAccepted = termsAccepted,
        onTermsAcceptedChange = { termsAccepted = it },
        isFormValid = isFormValid,
        onBackClick = onBackClick,
        onRegisterClick = onRegisterClick,
        onLoginClick = onLoginClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    MotorMatesTheme {
        RegisterScreen(
            onBackClick = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}
