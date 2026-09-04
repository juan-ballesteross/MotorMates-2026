package com.example.motormates.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val passwordsMatch = uiState.confirmPassword.isEmpty() || uiState.password == uiState.confirmPassword
    val isFormValid = uiState.fullName.isNotBlank() &&
        uiState.email.isNotBlank() &&
        uiState.password.isNotBlank() &&
        uiState.password == uiState.confirmPassword &&
        uiState.termsAccepted

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
