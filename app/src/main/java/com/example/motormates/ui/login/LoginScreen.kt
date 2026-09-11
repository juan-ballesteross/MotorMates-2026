package com.example.motormates.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isFormValid = uiState.email.isNotBlank() && uiState.password.isNotBlank()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginClick()
        }
    }

    LoginScreenContent(
        email = uiState.email,
        onEmailChange = viewModel::updateEmail,
        password = uiState.password,
        onPasswordChange = viewModel::updatePassword,
        passwordVisible = uiState.passwordVisible,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        isFormValid = isFormValid,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onLoginClick = viewModel::signIn,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MotorMatesTheme {
        LoginScreenContent(
            email = "jose@example.com",
            onEmailChange = {},
            password = "password",
            onPasswordChange = {},
            passwordVisible = false,
            onTogglePasswordVisibility = {},
            isFormValid = true,
            isLoading = false,
            errorMessage = null,
            onLoginClick = {},
            onRegisterClick = {},
            onForgotPasswordClick = {}
        )
    }
}
