package com.example.motormates.ui.register.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
import com.example.motormates.ui.theme.MotorMatesRed
import com.example.motormates.ui.theme.MotorMatesRedLight
import com.example.motormates.ui.theme.MotorMatesTextSecondary

@Composable
fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val linkStyle = SpanStyle(color = MotorMatesRedLight, fontWeight = FontWeight.SemiBold)
    val termsText = buildAnnotatedString {
        withStyle(SpanStyle(color = MotorMatesTextSecondary)) { append(stringResource(R.string.register_terms_accept_prefix)) }
        withStyle(linkStyle) { append(stringResource(R.string.register_terms_service)) }
        withStyle(SpanStyle(color = MotorMatesTextSecondary)) { append(stringResource(R.string.register_terms_and)) }
        withStyle(linkStyle) { append(stringResource(R.string.register_terms_privacy)) }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
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
