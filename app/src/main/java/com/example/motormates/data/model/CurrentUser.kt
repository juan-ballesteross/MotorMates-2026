package com.example.motormates.data.model

/**
 * Id del usuario "autenticado" contra el backend propio.
 *
 * Está quemado a propósito: los usuarios de Firebase Auth (login real de la
 * app) no tienen ninguna relación con la tabla users del backend, así que
 * toda la app asume que quien está usándola es el usuario 1. Cuando exista
 * esa relación, este es el único punto que hay que cambiar.
 */
const val CURRENT_USER_ID = 1
