package com.example.motormates.ui.login

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import com.example.motormates.ui.login.components.GoogleSignInButton
import com.example.motormates.ui.login.components.LoginDivider
import com.example.motormates.ui.login.components.LoginLogo
import com.example.motormates.ui.login.components.LoginTextField
import com.example.motormates.ui.theme.Elevation
import com.example.motormates.ui.theme.Motion
import com.example.motormates.ui.theme.rememberPressScale

@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isFormValid: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasAppeared = true }
    val appearProgress by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = Motion.durationMedium),
        label = "loginScreenAppear"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .testTag("loginScreen")
            .alpha(appearProgress)
            .offset(y = ((1f - appearProgress) * 24).dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        LoginLogo()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.login_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(28.dp))

        LoginTextField(
            label = stringResource(R.string.login_email_label),
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.login_email_placeholder),
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            testTag = "loginEmail"
        )
        Spacer(modifier = Modifier.height(16.dp))

        LoginTextField(
            label = stringResource(R.string.login_password_label),
            value = password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(R.string.login_password_placeholder),
            leadingIcon = Icons.Filled.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            testTag = "loginPassword"
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.login_forgot_password),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onForgotPasswordClick)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        val loginSubmitInteractionSource = remember { MutableInteractionSource() }
        val loginSubmitScale = rememberPressScale(loginSubmitInteractionSource)
        Button(
            onClick = onLoginClick,
            enabled = isFormValid && !isLoading,
            interactionSource = loginSubmitInteractionSource,
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
                .scale(loginSubmitScale)
                .testTag("loginSubmit")
        ) {
            Text(
                text = if (isLoading) {
                    stringResource(R.string.login_loading_button)
                } else {
                    stringResource(R.string.login_button)
                },
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        LoginDivider()
        Spacer(modifier = Modifier.height(20.dp))

        GoogleSignInButton(onClick = onGoogleSignInClick)
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.login_no_account_prefix),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(R.string.login_register_link),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRegisterClick)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
