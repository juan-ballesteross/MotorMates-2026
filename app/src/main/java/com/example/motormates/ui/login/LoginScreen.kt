package com.example.motormates.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesTheme
import kotlinx.coroutines.launch

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

    val context = LocalContext.current
    val webClientId = androidx.compose.ui.res.stringResource(id = R.string.default_web_client_id)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val handleGoogleSignIn = {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                val credential = result.credential
                
                // Usualmente el credentialManager retorna un GoogleIdTokenCredential 
                // cuando se escoge una cuenta de Google
                if (credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.onGoogleSignInResult(googleIdTokenCredential.idToken)
                } else {
                    viewModel.onGoogleSignInResult(null)
                }
            } catch (e: GetCredentialException) {
                // Hay tres casos aquí: el usuario lo cerró (CustomException/CancelException),
                // falló la red, o falló la verificación de Google (NoCredentialException/ApiException).
                val errorMsg = if (e is androidx.credentials.exceptions.NoCredentialException) {
                    "No se encontró una cuenta de Google en este dispositivo"
                } else if (e is androidx.credentials.exceptions.GetCredentialCustomException || e.message?.contains("canceled", ignoreCase = true) == true) {
                    null // Se canceló, no mostrar error feo.
                } else {
                    "Tu app no tiene el certificado correcto de Google ($e)" 
                }
                viewModel.onGoogleSignInError(errorMsg)
            }
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
        onGoogleSignInClick = { handleGoogleSignIn() },
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
            onGoogleSignInClick = {},
            onRegisterClick = {},
            onForgotPasswordClick = {}
        )
    }
}
