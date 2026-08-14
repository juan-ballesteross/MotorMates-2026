package com.example.motormates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.motormates.ui.screens.SearchScreen
import com.example.motormates.ui.theme.MotorMatesTheme

// Nota: para esta entrega, esta rama (Window-Search) solo muestra la pantalla
// de Busqueda, que es la que le corresponde a este integrante del equipo.
// El flujo completo con Login/Register/Feed sigue disponible en la rama
// principal y en las pantallas de los demas integrantes; se debe restaurar
// aqui cuando se junten todas las pantallas del equipo.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotorMatesTheme {
                SearchScreen()
            }
        }
    }
}
