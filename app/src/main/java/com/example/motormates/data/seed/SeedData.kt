package com.example.motormates.data.seed

import com.example.motormates.R
import com.example.motormates.data.dto.UserDto
import com.example.motormates.data.dto.VehicleFirestoreDto

/**
 * Datos quemados para Firestore. Son los mismos que siembra el backend
 * (initUsers.js, initVehicles.js, initReviews.js) para que la app muestre
 * lo mismo con cualquiera de las dos fuentes de datos.
 *
 * Los ids son fijos a propósito: si el seed se ejecuta dos veces, set()
 * sobrescribe los mismos documentos en vez de duplicarlos. Los vehículos
 * usan "1".."8", los mismos ids que el buscador (SearchMocks).
 */
internal object SeedData {

    const val SEED_USER_RODRIGO = "seed_user_1"
    const val SEED_USER_SOFIA = "seed_user_2"
    const val SEED_USER_IVAN = "seed_user_3"

    val users = listOf(
        seedUser(SEED_USER_RODRIGO, "Rodrigo Salinas", "rodrigo@motormates.com", "Bogotá, Colombia", "Fan de los deportivos y los track days."),
        seedUser(SEED_USER_SOFIA, "Sofía Reyes", "sofia@motormates.com", "Medellín, Colombia", "Clásicos los domingos, eléctrico entre semana."),
        seedUser(SEED_USER_IVAN, "Iván Pérez", "ivan@motormates.com", "Cali, Colombia", "Si tiene 4x4, lo saco a trocha.")
    )

    val vehicles = listOf(
        VehicleFirestoreDto(
            id = "1", brand = "Porsche", model = "911 GT3", year = 2024, category = "Deportivo",
            imageUrl = "https://www.europeanprestige.co.uk/blobs/stock/577/images/45f3295c-923a-40fd-b1e7-6f6d0d129de2/hi4a4328.jpg?width=2000&height=1333",
            potencia = "502 hp", aceleracion = "3.4 s", velocidadMaxima = "318 km/h", traccion = "RWD"
        ),
        VehicleFirestoreDto(
            id = "2", brand = "Toyota", model = "Land Cruiser", year = 2022, category = "SUV",
            imageUrl = "https://autoamerica.com.co/wp-content/uploads/2020/08/4N4A2472.jpg",
            potencia = "409 hp", aceleracion = "6.7 s", velocidadMaxima = "210 km/h", traccion = "4x4"
        ),
        VehicleFirestoreDto(
            id = "3", brand = "Ford", model = "Mustang '67", year = 1967, category = "Clásico",
            imageUrl = "https://bringatrailer.com/wp-content/uploads/2026/07/1967_ford_mustang-gt_1967_ford_mustang-gt_445183c0-f15d-4fac-bfaf-aab6c2d8d11d-uhtvDu-46102-46103-scaled.jpg?w=940",
            potencia = "320 hp", aceleracion = "6.5 s", velocidadMaxima = "195 km/h", traccion = "RWD"
        ),
        VehicleFirestoreDto(
            id = "4", brand = "Tesla", model = "Model 3", year = 2024, category = "Eléctrico",
            imageUrl = "https://cdn.motor1.com/images/mgl/eoOk4M/s1/prueba-tesla-model-3-traccion-trasera.jpg",
            potencia = "283 hp", aceleracion = "6.1 s", velocidadMaxima = "201 km/h", traccion = "RWD"
        ),
        VehicleFirestoreDto(
            id = "5", brand = "Chevrolet", model = "Camaro SS", year = 2021, category = "Deportivo",
            imageUrl = "https://cdn-fastly.autoguide.com/media/2023/06/07/12309463/2016-chevrolet-camaro-ss-review-curbed-with-craig-cole.jpg?size=720x845&nocrop=1",
            potencia = "455 hp", aceleracion = "4.0 s", velocidadMaxima = "250 km/h", traccion = "RWD"
        ),
        VehicleFirestoreDto(
            id = "6", brand = "Jeep", model = "Wrangler", year = 2023, category = "SUV",
            imageUrl = "https://orangeclub.co/wp-content/uploads/2025/05/Jeep-Wrangler-Rubicon.jpg",
            potencia = "285 hp", aceleracion = "6.9 s", velocidadMaxima = "180 km/h", traccion = "4x4"
        ),
        VehicleFirestoreDto(
            id = "7", brand = "Volkswagen", model = "Beetle '65", year = 1965, category = "Clásico",
            imageUrl = "https://goauctionmpc.goauction.co.uk/stock/1708-0-medium.jpg?v=63829760351190",
            potencia = "50 hp", aceleracion = "27.0 s", velocidadMaxima = "125 km/h", traccion = "RWD"
        ),
        // El 8 (Ford Ranger) se deja sin reseñas a propósito, para comprobar
        // que el detalle muestra calificación 0 y 0 reseñas.
        VehicleFirestoreDto(
            id = "8", brand = "Ford", model = "Ranger", year = 2022, category = "Pickup",
            imageUrl = "https://imagenes.portafolio.co/files/image_600_455/uploads/2021/01/29/6014dcacba87b.jpeg",
            potencia = "210 hp", aceleracion = "10.3 s", velocidadMaxima = "180 km/h", traccion = "4x4"
        )
    )

    /** (autor, vehículo, rating, comentario), igual que initReviews.js. */
    val reviews = listOf(
        SeedReview(SEED_USER_RODRIGO, "1", 5, "El sonido del motor es otro nivel. Se siente pegado al piso en cada curva."),
        SeedReview(SEED_USER_SOFIA, "1", 5, "Impecable en pista, pero para ciudad es incómodo y muy duro."),
        SeedReview(SEED_USER_IVAN, "1", 4, "Una máquina, aunque el precio del mantenimiento asusta."),
        SeedReview(SEED_USER_RODRIGO, "2", 4, "Lo saqué a trocha todo un fin de semana y no se quejó ni una vez."),
        SeedReview(SEED_USER_IVAN, "2", 3, "Enorme y confiable, pero el consumo de gasolina es alto."),
        SeedReview(SEED_USER_SOFIA, "3", 5, "Un clásico que voltea cabezas donde sea. Vale cada peso de la restauración."),
        SeedReview(SEED_USER_IVAN, "3", 4, "Hermoso, aunque sin frenos modernos hay que manejarlo con respeto."),
        SeedReview(SEED_USER_RODRIGO, "4", 4, "El ahorro mensual se nota muchísimo. La autonomía me alcanza de sobra."),
        SeedReview(SEED_USER_SOFIA, "4", 3, "Buen carro, pero encontrar cargador rápido en carretera todavía cuesta."),
        SeedReview(SEED_USER_IVAN, "5", 4, "Potencia de sobra y buen precio para lo que entrega."),
        SeedReview(SEED_USER_RODRIGO, "6", 5, "Quitarle el techo un domingo no tiene comparación. Perfecto para montaña."),
        SeedReview(SEED_USER_SOFIA, "6", 3, "Divertidísimo fuera de la ciudad, pero en autopista suena mucho."),
        SeedReview(SEED_USER_SOFIA, "7", 4, "Sencillo, económico y se arregla con cualquier mecánico. Un amor.")
    )

    data class SeedReview(
        val userId: String,
        val vehicleId: String,
        val rating: Int,
        val comment: String
    )

    private fun seedUser(id: String, fullName: String, email: String, location: String, bio: String) =
        UserDto(
            id = id,
            fullName = fullName,
            email = email,
            username = email.substringBefore("@"),
            location = location,
            bio = bio,
            createdAt = System.currentTimeMillis()
        )
}

/**
 * Los usuarios semilla no tienen foto subida a Storage, así que a cada uno
 * se le asigna una foto local fija (la misma que usan las Historias del
 * Feed), para no mostrar siempre el mismo avatar sin importar de quién sea.
 */
fun localAvatarResFor(userId: String): Int? = when (userId) {
    SeedData.SEED_USER_RODRIGO -> R.drawable.user_3
    SeedData.SEED_USER_SOFIA -> R.drawable.user1
    SeedData.SEED_USER_IVAN -> R.drawable.user2
    else -> null
}
