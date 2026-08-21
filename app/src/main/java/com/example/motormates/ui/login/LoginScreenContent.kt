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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
<<<<<<< HEAD
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
=======
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
>>>>>>> origin/master
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
<<<<<<< HEAD
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesRedLight
import com.example.motormates.ui.theme.MotorMatesTextSecondary
=======
>>>>>>> origin/master

@Composable
fun LoginScreenContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isFormValid: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
<<<<<<< HEAD
            .background(MotorMatesBackground)
=======
            .background(MaterialTheme.colorScheme.background)
>>>>>>> origin/master
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        LoginLogo()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.login_title),
<<<<<<< HEAD
            color = Color.White,
=======
            color = MaterialTheme.colorScheme.onBackground,
>>>>>>> origin/master
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
<<<<<<< HEAD
            color = MotorMatesTextSecondary,
=======
            color = MaterialTheme.colorScheme.onSurfaceVariant,
>>>>>>> origin/master
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        LoginTextField(
            label = stringResource(R.string.login_email_label),
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.login_email_placeholder),
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
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
            onTogglePasswordVisibility = onTogglePasswordVisibility
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.login_forgot_password),
<<<<<<< HEAD
                color = MotorMatesRedLight,
=======
                color = MaterialTheme.colorScheme.secondary,
>>>>>>> origin/master
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onForgotPasswordClick)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLoginClick,
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
<<<<<<< HEAD
                containerColor = MotorMatesRed,
                disabledContainerColor = MotorMatesRed.copy(alpha = 0.4f)
=======
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
>>>>>>> origin/master
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
<<<<<<< HEAD
            Text(text = stringResource(R.string.login_button), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
=======
            Text(
                text = stringResource(R.string.login_button),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
>>>>>>> origin/master
        }
        Spacer(modifier = Modifier.height(20.dp))

        LoginDivider()
        Spacer(modifier = Modifier.height(20.dp))

        GoogleSignInButton(onClick = {})
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
<<<<<<< HEAD
            Text(text = stringResource(R.string.login_no_account_prefix), color = MotorMatesTextSecondary, fontSize = 14.sp)
            Text(
                text = stringResource(R.string.login_register_link),
                color = MotorMatesRedLight,
=======
            Text(
                text = stringResource(R.string.login_no_account_prefix),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(R.string.login_register_link),
                color = MaterialTheme.colorScheme.secondary,
>>>>>>> origin/master
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRegisterClick)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
