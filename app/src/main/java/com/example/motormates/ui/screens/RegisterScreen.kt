package com.example.motormates.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesRedLight
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary

/**
 * Pantalla de registro. El botón "Crear cuenta" solo se habilita cuando el
 * formulario es válido (campos completos, contraseñas coincidentes y
 * términos aceptados).
 */
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(true) }

    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
    val isFormValid = fullName.isNotBlank() &&
        email.isNotBlank() &&
        password.isNotBlank() &&
        password == confirmPassword &&
        termsAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        RegisterTopBar(onBackClick = onBackClick)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Únete a MotorMates",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Comparte tu pasión por los autos con miles de entusiastas",
            color = MotorMatesTextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        RegisterTextField(
            label = "Nombre completo",
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Tu nombre"
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = "Correo electrónico",
            value = email,
            onValueChange = { email = it },
            placeholder = "tucorreo@ejemplo.com",
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = "Contraseña",
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••",
            isPassword = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        RegisterTextField(
            label = "Confirmar contraseña",
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "••••••••",
            isPassword = true,
            isError = !passwordsMatch
        )
        Spacer(modifier = Modifier.height(20.dp))

        TermsCheckbox(
            checked = termsAccepted,
            onCheckedChange = { termsAccepted = it }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRegisterClick,
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MotorMatesRed,
                disabledContainerColor = MotorMatesRed.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "Crear cuenta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "¿Ya tienes cuenta? ", color = MotorMatesTextSecondary, fontSize = 14.sp)
            Text(
                text = "Inicia sesión",
                color = MotorMatesRedLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onLoginClick)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }
        Text(
            text = "Crear cuenta",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Campo de texto con la etiqueta arriba y fondo de superficie, siguiendo
 * la paleta oscura del resto de la app.
 */
@Composable
private fun RegisterTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = MotorMatesTextSecondary) },
            singleLine = true,
            isError = isError,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MotorMatesSurface,
                unfocusedContainerColor = MotorMatesSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MotorMatesRed,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MotorMatesRed,
                cursorColor = MotorMatesRed
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TermsCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val linkStyle = SpanStyle(color = MotorMatesRedLight, fontWeight = FontWeight.SemiBold)
    val termsText = buildAnnotatedString {
        withStyle(SpanStyle(color = MotorMatesTextSecondary)) { append("Acepto los ") }
        withStyle(linkStyle) { append("Términos de servicio") }
        withStyle(SpanStyle(color = MotorMatesTextSecondary)) { append(" y la ") }
        withStyle(linkStyle) { append("Política de privacidad") }
    }

    Row(verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MotorMatesRed,
                uncheckedColor = MotorMatesTextSecondary,
                checkmarkColor = Color.White
            )
        )
        Text(
            text = termsText,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(top = 14.dp)
                .weight(1f)
        )
    }
}
