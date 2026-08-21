package com.example.motormates.ui.login.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R

@Composable
fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = "G", color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.login_google_button),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
