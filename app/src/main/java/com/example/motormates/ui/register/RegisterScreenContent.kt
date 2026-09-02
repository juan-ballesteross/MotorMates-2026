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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.register.components.RegisterTextField
import com.example.motormates.ui.register.components.RegisterTopBar
import com.example.motormates.ui.register.components.TermsCheckbox

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
    passwordsMatch: Boolean,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    isFormValid: Boolean,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        RegisterTopBar(onBackClick = onBackClick)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.register_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.register_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        RegisterTextField(
            label = stringResource(R.string.register_fullname_label),
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = stringResource(R.string.register_fullname_placeholder)
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = stringResource(R.string.register_email_label),
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.register_email_placeholder),
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = stringResource(R.string.register_password_label),
            value = password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(R.string.register_password_placeholder),
            isPassword = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = stringResource(R.string.register_confirm_password_label),
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = stringResource(R.string.register_password_placeholder),
            isPassword = true,
            isError = !passwordsMatch
        )
        Spacer(modifier = Modifier.height(20.dp))

        TermsCheckbox(
            checked = termsAccepted,
            onCheckedChange = onTermsAcceptedChange
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRegisterClick,
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = stringResource(R.string.register_button), color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
