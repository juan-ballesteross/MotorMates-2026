package com.example.motormates.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Pantalla de registro. Ya no guarda estado con remember — los campos viven
 * en RegisterViewModel como StateFlow. passwordsMatch/isFormValid se calculan
 * aquí mismo porque son valores derivados (no necesitan su propio flow),
 * igual que isFormValid en LoginScreen.
 */
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val termsAccepted by viewModel.termsAccepted.collectAsStateWithLifecycle()

    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
    val isFormValid = fullName.isNotBlank() &&
        email.isNotBlank() &&
        password.isNotBlank() &&
        password == confirmPassword &&
        termsAccepted

    RegisterScreenContent(
        fullName = fullName,
        onFullNameChange = viewModel::updateFullName,
        email = email,
        onEmailChange = viewModel::updateEmail,
        password = password,
        onPasswordChange = viewModel::updatePassword,
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = viewModel::updateConfirmPassword,
        passwordsMatch = passwordsMatch,
        termsAccepted = termsAccepted,
        onTermsAcceptedChange = viewModel::updateTermsAccepted,
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
