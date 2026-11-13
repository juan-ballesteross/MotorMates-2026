package com.example.motormates.data.model

import android.net.Uri

/**
 * Modelo de dominio de una reseña. Los ids son String porque en Firestore
 * el id de un documento es un texto (y el userId es el uid de Firebase Auth).
 * author y vehicle son nulables: son la "captura" desnormalizada que se
 * guarda dentro de la reseña, y una fuente de datos puede no traerla.
 */
data class Review(
    val id: String,
    val rating: Int,
    val comment: String?,
    val userId: String,
    val vehicleId: String,
    val createdAt: String?,
    val likesCount: Int = 0,
    val author: ReviewAuthor?,
    val vehicle: ReviewVehicleRef?,
    /**
     * Dónde se publicó la reseña, para pintarla en el mapa. Nulable porque es
     * opcional: si el usuario niega el permiso de ubicación la reseña se
     * publica igual, y las reseñas creadas antes de esta función no la tienen.
     */
    val location: ReviewLocation? = null,
    val imageUrls: List<String> = emptyList()
)

data class ReviewAuthor(val id: String, val fullName: String, val avatarUrl: String? = null)

data class ReviewVehicleRef(val id: String, val brand: String, val model: String)

/**
 * Dónde se publicó una reseña.
 *
 * Las coordenadas se guardan como Double y no como el LatLng de Maps a
 * propósito: data.model lo consumen las pruebas unitarias de JVM puro, y
 * LatLng arrastraría el SDK de Android (y Robolectric) a los tests de los
 * mappers. La conversión a LatLng se hace en la capa de UI.
 *
 * placeName es el nombre legible del sitio ("Chapinero, Bogotá"). Se resuelve
 * UNA vez, al publicar, y viaja aquí dentro para quedar guardado junto a las
 * coordenadas: es la misma idea de la copia desnormalizada del autor y del
 * vehículo. Resolverlo al leer costaría una llamada de red por reseña en
 * pantalla. Es nulable porque el geocodificador puede no tener respuesta, y
 * porque las reseñas publicadas antes de esta función no lo tienen.
 */
data class ReviewLocation(
    val lat: Double,
    val lng: Double,
    val placeName: String? = null
)

/**
 * Lo que el repositorio le entrega a la fuente de datos para crear una
 * reseña: además de los datos propios, lleva el nombre/foto del autor y la
 * marca/modelo del vehículo para guardarlos desnormalizados (sin JOINs).
 */
data class NewReview(
    val userId: String,
    val vehicleId: String,
    val rating: Int,
    val comment: String?,
    val authorName: String,
    val authorAvatarUrl: String?,
    val vehicleBrand: String,
    val vehicleModel: String,
    /** Null si no se pudo o no se quiso capturar la ubicación. */
    val location: ReviewLocation? = null,
    val imageUris: List<Uri> = emptyList()
)
