package com.example.motormates.data.model

import com.example.motormates.R

/**
 * Id del usuario "autenticado" contra el backend propio.
 *
 * Está quemado a propósito: los usuarios de Firebase Auth (login real de la
 * app) no tienen ninguna relación con la tabla users del backend, así que
 * toda la app asume que quien está usándola es el usuario 1. Cuando exista
 * esa relación, este es el único punto que hay que cambiar.
 */
const val CURRENT_USER_ID = 1

/**
 * Los 3 usuarios que siembra initUsers.js en el backend. El backend no
 * guarda foto de perfil, así que a cada uno se le asigna una foto local
 * fija (mismo criterio que ya usan las Historias del Feed), para no
 * mostrar siempre el mismo avatar sin importar de quién sea.
 */
fun localAvatarResFor(userId: Int): Int? = when (userId) {
    1 -> R.drawable.user_3 // Rodrigo Salinas
    2 -> R.drawable.user1  // Sofía Reyes
    3 -> R.drawable.user2  // Iván Pérez
    else -> null
}
