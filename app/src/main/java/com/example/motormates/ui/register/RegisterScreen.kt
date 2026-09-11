package com.example.motormates.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val passwordsMatch = uiState.confirmPassword.isEmpty() || uiState.password == uiState.confirmPassword
    val isFormValid = uiState.fullName.isNotBlank() &&
        uiState.email.isNotBlank() &&
        uiState.password.isNotBlank() &&
        uiState.password == uiState.confirmPassword &&
        uiState.termsAccepted

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegisterClick()
        }
    }

    RegisterScreenContent(
        fullName = uiState.fullName,
        onFullNameChange = viewModel::updateFullName,
        email = uiState.email,
        onEmailChange = viewModel::updateEmail,
        password = uiState.password,
        onPasswordChange = viewModel::updatePassword,
        confirmPassword = uiState.confirmPassword,
        onConfirmPasswordChange = viewModel::updateConfirmPassword,
        passwordsMatch = passwordsMatch,
        termsAccepted = uiState.termsAccepted,
        onTermsAcceptedChange = viewModel::updateTermsAccepted,
        isFormValid = isFormValid,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onBackClick = onBackClick,
        onRegisterClick = viewModel::signUp,
        onLoginClick = onLoginClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    MotorMatesTheme {
        RegisterScreenContent(
            fullName = "Jose",
            onFullNameChange = {},
            email = "jose@example.com",
            onEmailChange = {},
            password = "password",
            onPasswordChange = {},
            confirmPassword = "password",
            onConfirmPasswordChange = {},
            passwordsMatch = true,
            termsAccepted = true,
            onTermsAcceptedChange = {},
            isFormValid = true,
            isLoading = false,
            errorMessage = null,
            onBackClick = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}
