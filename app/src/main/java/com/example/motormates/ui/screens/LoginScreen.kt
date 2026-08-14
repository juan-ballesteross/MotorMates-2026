package com.example.motormates.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.ui.theme.MotorMatesBackground
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesTextSecondary

@Composable
fun LoginScreen(
    onLoginClick: (
        nombreCompleto: String,
        correo: String,
        contrasena: String
    ) -> Unit,
    onBackClick: () -> Unit = {},
    onTerminosClick: () -> Unit = {},
    onPrivacidadClick: () -> Unit = {}
) {
    var nombreCompleto by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var aceptaTerminos by remember { mutableStateOf(false) }

    val puedeCrearCuenta = nombreCompleto.isNotBlank() &&
        correo.isNotBlank() &&
        contrasena.isNotBlank() &&
        contrasena == confirmarContrasena &&
        aceptaTerminos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MotorMatesBackground)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Flecha de regreso — placeholder de texto para no depender de Icons.
        // Si quieres el ícono real de Material, créalo con res -> New -> Vector Asset
        // (busca "arrow_back") y reemplaza el Text de abajo por:
        // Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver", tint = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Text("‹", color = Color.White, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                text = "Crear cuenta",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Únete a MotorMates",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Comparte tu pasión por los autos con miles de entusiastas",
            color = MotorMatesTextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        CampoConLabel(
            label = "Nombre completo",
            value = nombreCompleto,
            onValueChange = { nombreCompleto = it },
            placeholder = "Tu nombre"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoConLabel(
            label = "Correo electrónico",
            value = correo,
            onValueChange = { correo = it },
            placeholder = "tucorreo@ejemplo.com"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoConLabel(
            label = "Contraseña",
            value = contrasena,
            onValueChange = { contrasena = it },
            placeholder = "••••••••••",
            esPassword = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        CampoConLabel(
            label = "Confirmar contraseña",
            value = confirmarContrasena,
            onValueChange = { confirmarContrasena = it },
            placeholder = "••••••••••",
            esPassword = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = aceptaTerminos,
                onCheckedChange = { aceptaTerminos = it },
                colors = CheckboxDefaults.colors(checkedColor = MotorMatesRed)
            )

            val textoTerminos = buildAnnotatedString {
                append("Acepto los ")
                pushStringAnnotation(tag = "TERMINOS", annotation = "terminos")
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                    append("Términos de servicio")
                }
                pop()
                append(" y la ")
                pushStringAnnotation(tag = "PRIVACIDAD", annotation = "privacidad")
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                    append("Política de privacidad")
                }
                pop()
            }

            ClickableText(
                text = textoTerminos,
                style = androidx.compose.ui.text.TextStyle(
                    color = MotorMatesTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.padding(top = 12.dp),
                onClick = { offset ->
                    textoTerminos.getStringAnnotations("TERMINOS", offset, offset)
                        .firstOrNull()?.let { onTerminosClick() }
                    textoTerminos.getStringAnnotations("PRIVACIDAD", offset, offset)
                        .firstOrNull()?.let { onPrivacidadClick() }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLoginClick(nombreCompleto, correo, contrasena) },
            enabled = puedeCrearCuenta,
            colors = ButtonDefaults.buttonColors(containerColor = MotorMatesRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "Crear cuenta", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CampoConLabel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    esPassword: Boolean = false
) {
    Column {
        Text(text = label, color = MotorMatesTextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MotorMatesTextSecondary) },
            singleLine = true,
            visualTransformation = if (esPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1C1C1E),
                unfocusedContainerColor = Color(0xFF1C1C1E),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MotorMatesRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}
