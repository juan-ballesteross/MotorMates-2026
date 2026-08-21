package com.example.motormates.ui.login.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
<<<<<<< HEAD
=======
import androidx.compose.material3.MaterialTheme
>>>>>>> origin/master
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.R
<<<<<<< HEAD
import com.example.motormates.ui.theme.MotorMatesSurface
import com.example.motormates.ui.theme.MotorMatesTextSecondary
=======
>>>>>>> origin/master

@Composable
fun LoginDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
<<<<<<< HEAD
        HorizontalDivider(modifier = Modifier.weight(1f), color = MotorMatesSurface)
        Text(
            text = stringResource(R.string.login_or_divider),
            color = MotorMatesTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MotorMatesSurface)
=======
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface)
        Text(
            text = stringResource(R.string.login_or_divider),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface)
>>>>>>> origin/master
    }
}
