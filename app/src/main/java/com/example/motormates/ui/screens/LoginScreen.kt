package com.example.motormates.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesRedLight
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary
import com.example.motormates.ui.theme.MotorMatesTheme
import androidx.compose.foundation.Image

/**
 * Pantalla de inicio de sesión. El botón "Iniciar sesión" solo se habilita
 * cuando el correo y la contraseña tienen contenido.
 */
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isFormValid = email.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        LogoBadge()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Bienvenido de nuevo",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Inicia sesión para seguir conectado con la comunidad",
            color = MotorMatesTextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        LoginTextField(
            label = "Correo electrónico",
            value = email,
            onValueChange = { email = it },
            placeholder = "tucorreo@ejemplo.com",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))

        LoginTextField(
            label = "Contraseña",
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••••",
            leadingIcon = Icons.Filled.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible }
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = MotorMatesRedLight,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onForgotPasswordClick)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLoginClick,
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MotorMatesRed,
                disabledContainerColor = MotorMatesRed.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "Iniciar sesión", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))

        OrDivider()
        Spacer(modifier = Modifier.height(20.dp))

        GoogleButton(onClick = {})
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "¿No tienes cuenta? ", color = MotorMatesTextSecondary, fontSize = 14.sp)
            Text(
                text = "Regístrate",
                color = MotorMatesRedLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRegisterClick)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LogoBadge() {
    // ContentScale.Crop escala la imagen para llenar el ancho y recorta
    // el sobrante vertical (el espacio en blanco arriba/abajo del PNG),
    // logrando que el logo se vea más grande dentro del mismo contenedor.
    Image(
        painter = painterResource(id = R.drawable.logo_motormates),
        contentDescription = "Logo de MotorMates",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(160.dp)
            .height(70.dp)
    )
}

/**
 * Campo de texto con etiqueta arriba, ícono inicial y fondo de superficie,
 * siguiendo la paleta oscura del resto de la app.
 */
@Composable
private fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = MotorMatesTextSecondary) },
            leadingIcon = {
                Icon(imageVector = leadingIcon, contentDescription = null, tint = MotorMatesTextSecondary)
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = MotorMatesTextSecondary
                        )
                    }
                }
            } else null,
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MotorMatesSurface,
                unfocusedContainerColor = MotorMatesSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MotorMatesRed,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MotorMatesRed
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MotorMatesSurface)
        Text(
            text = "o continúa con",
            color = MotorMatesTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MotorMatesSurface)
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MotorMatesSurface),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = "G", color = MotorMatesRedLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Continuar con Google", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MotorMatesTheme {
        LoginScreen(onLoginClick = {})
    }
}
