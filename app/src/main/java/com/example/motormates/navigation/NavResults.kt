package com.example.motormates.navigation

/**
 * Bandera que la pantalla de nueva/editar reseña deja en el
 * savedStateHandle de la entrada anterior del back stack antes de cerrarse,
 * para que el detalle sepa que debe recargar su lista de reseñas.
 * Es el patrón de "resultado" de Navigation Compose: no hace falta estado
 * compartido entre pantallas.
 */
const val RESULT_REVIEWS_CHANGED = "reviewsChanged"
