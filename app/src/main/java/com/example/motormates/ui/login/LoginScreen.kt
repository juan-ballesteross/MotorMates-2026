package com.example.motormates.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isFormValid = uiState.email.isNotBlank() && uiState.password.isNotBlank()

    LoginScreenContent(
        email = uiState.email,
        onEmailChange = viewModel::updateEmail,
        password = uiState.password,
        onPasswordChange = viewModel::updatePassword,
        passwordVisible = uiState.passwordVisible,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        isFormValid = isFormValid,
        onLoginClick = onLoginClick,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MotorMatesTheme {
        LoginScreen()
    }
}
