package com.example.motormates.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.register.components.RegisterTextField
import com.example.motormates.ui.register.components.RegisterTopBar
import com.example.motormates.ui.register.components.TermsCheckbox
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Motion
import com.example.motormates.ui.theme.rememberPressScale

@Composable
fun RegisterScreenContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    confirmPasswordVisible: Boolean,
    onToggleConfirmPasswordVisibility: () -> Unit,
    passwordsMatch: Boolean,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    isFormValid: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasAppeared = true }
    val appearProgress by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = Motion.durationMedium),
        label = "registerScreenAppear"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .testTag("registerScreen")
            .alpha(appearProgress)
            .offset(y = ((1f - appearProgress) * 24).dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        RegisterTopBar(onBackClick = onBackClick)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.register_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.register_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(28.dp))

        RegisterTextField(
            label = stringResource(R.string.register_fullname_label),
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = stringResource(R.string.register_fullname_placeholder),
            testTag = "registerFullName"
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = stringResource(R.string.register_email_label),
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.register_email_placeholder),
            keyboardType = KeyboardType.Email,
            testTag = "registerEmail"
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = stringResource(R.string.register_password_label),
            value = password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(R.string.register_password_placeholder),
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            testTag = "registerPassword"
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = stringResource(R.string.register_confirm_password_label),
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = stringResource(R.string.register_password_placeholder),
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onTogglePasswordVisibility = onToggleConfirmPasswordVisibility,
            isError = !passwordsMatch,
            testTag = "registerConfirmPassword"
        )
        Spacer(modifier = Modifier.height(20.dp))

        TermsCheckbox(
            checked = termsAccepted,
            onCheckedChange = onTermsAcceptedChange
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("registerError")
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        val registerSubmitInteractionSource = remember { MutableInteractionSource() }
        val registerSubmitScale = rememberPressScale(registerSubmitInteractionSource)
        Button(
            onClick = onRegisterClick,
            enabled = isFormValid && !isLoading,
            interactionSource = registerSubmitInteractionSource,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = Elevation.sm,
                pressedElevation = Elevation.xs,
                disabledElevation = Elevation.none
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(registerSubmitScale)
                .testTag("registerSubmit")
        ) {
            Text(
                text = if (isLoading) {
                    stringResource(R.string.register_loading_button)
                } else {
                    stringResource(R.string.register_button)
                },
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(R.string.register_has_account_prefix), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(
                text = stringResource(R.string.register_login_link),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onLoginClick)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
