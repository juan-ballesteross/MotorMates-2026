package com.example.motormates.navigation

/**
 * Bandera que la pantalla de nueva/editar reseña deja en el
 * savedStateHandle de la entrada anterior del back stack antes de cerrarse,
 * para que el detalle sepa que debe recargar su lista de reseñas.
 * Es el patrón de "resultado" de Navigation Compose: no hace falta estado
 * compartido entre pantallas.
 */
const val RESULT_REVIEWS_CHANGED = "reviewsChanged"

/**
 * Misma idea que RESULT_REVIEWS_CHANGED, pero para cuando se edita el
 * perfil propio: le avisa a "Mi perfil" que debe recargar los datos.
 */
const val RESULT_PROFILE_CHANGED = "profileChanged"

/**
 * Misma idea, para cuando se publica, edita o cancela un evento: le avisa al
 * detalle que debe recargar. El mapa no lo necesita, porque escucha Firestore
 * en vivo.
 */
const val RESULT_EVENTS_CHANGED = "eventsChanged"

/**
 * El punto que la pantalla de "elegir en el mapa" deja en la entrada anterior
 * del back stack.
 *
 * Van dos claves y no un objeto porque el savedStateHandle solo guarda lo que
 * entra en un Bundle: un Double sí, un data class propio no sin Parcelable, y
 * en este proyecto no hay ni Parcelable ni kotlinx-serialization.
 */
const val RESULT_PICKED_LAT = "pickedLat"
const val RESULT_PICKED_LNG = "pickedLng"

/**
 * El nombre del sitio que el geocodificador le resolvió al punto elegido.
 * Viaja junto a las coordenadas para que el formulario pueda proponerlo sin
 * volver a geocodificar.
 */
const val RESULT_PICKED_PLACE_NAME = "pickedPlaceName"
