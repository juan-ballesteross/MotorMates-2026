package com.example.motormates.ui.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
<<<<<<< HEAD
=======
import androidx.compose.material3.MaterialTheme
>>>>>>> origin/master
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
<<<<<<< HEAD
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary

/**
 * Campo de texto con etiqueta arriba, ícono inicial y fondo de superficie,
 * siguiendo la paleta oscura del resto de la app.
=======

/**
 * Campo de texto con etiqueta arriba, ícono inicial y fondo de superficie.
 * Usa MaterialTheme.colorScheme para que responda al modo claro/oscuro.
>>>>>>> origin/master
 */
@Composable
fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
<<<<<<< HEAD
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
=======
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
>>>>>>> origin/master
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
<<<<<<< HEAD
            placeholder = { Text(text = placeholder, color = MotorMatesTextSecondary) },
            leadingIcon = {
                Icon(imageVector = leadingIcon, contentDescription = null, tint = MotorMatesTextSecondary)
=======
            placeholder = { Text(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(imageVector = leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
>>>>>>> origin/master
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) {
                                stringResource(R.string.login_password_hide_cd)
                            } else {
                                stringResource(R.string.login_password_show_cd)
                            },
<<<<<<< HEAD
                            tint = MotorMatesTextSecondary
=======
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
>>>>>>> origin/master
                        )
                    }
                }
            } else null,
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
<<<<<<< HEAD
                focusedContainerColor = MotorMatesSurface,
                unfocusedContainerColor = MotorMatesSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MotorMatesRed,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MotorMatesRed
=======
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
>>>>>>> origin/master
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> origin/master
