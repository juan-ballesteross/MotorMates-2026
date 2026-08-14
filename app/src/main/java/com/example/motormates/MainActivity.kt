package com.example.motormates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.motormates.ui.screens.FeedScreen
import com.example.motormates.ui.screens.LoginScreen
import com.example.motormates.ui.theme.MotorMatesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotorMatesTheme {
                MotorMatesApp()
            }
        }
    }
}

@Composable
fun MotorMatesApp() {
    var isLoggedIn by remember { mutableStateOf(false) }

    if (isLoggedIn) {
        FeedScreen()
    } else {
        LoginScreen(onLoginClick = { isLoggedIn = true })
    }
}
