package com.example.motormates.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motormates.ui.theme.MotorMatesTheme

/**
 * Ya no guarda estado con remember — email/password/passwordVisible
 * viven en LoginViewModel como StateFlow. isFormValid se calcula aquí
 * mismo porque es un valor derivado (no necesita su propio flow).
 * onLoginClick/onRegisterClick/onForgotPasswordClick siguen siendo
 * callbacks de navegación pasados desde afuera — eso no le corresponde
 * al ViewModel, sino a quien conecte la navegación.
 */
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val passwordVisible by viewModel.passwordVisible.collectAsStateWithLifecycle()

    val isFormValid = email.isNotBlank() && password.isNotBlank()

    LoginScreenContent(
        email = email,
        onEmailChange = viewModel::updateEmail,
        password = password,
        onPasswordChange = viewModel::updatePassword,
        passwordVisible = passwordVisible,
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
