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
 * usan "1".."29"; los ocho primeros son los mismos ids del buscador
 * (SearchMocks).
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
            potencia = "502 hp", aceleracion = "3.4 s", velocidadMaxima = "318 km/h", traccion = "RWD",
            ownerId = SEED_USER_RODRIGO
        ),
        VehicleFirestoreDto(
            id = "2", brand = "Toyota", model = "Land Cruiser", year = 2022, category = "SUV",
            imageUrl = "https://autoamerica.com.co/wp-content/uploads/2020/08/4N4A2472.jpg",
            potencia = "409 hp", aceleracion = "6.7 s", velocidadMaxima = "210 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "3", brand = "Ford", model = "Mustang '67", year = 1967, category = "Clásico",
            imageUrl = "https://bringatrailer.com/wp-content/uploads/2026/07/1967_ford_mustang-gt_1967_ford_mustang-gt_445183c0-f15d-4fac-bfaf-aab6c2d8d11d-uhtvDu-46102-46103-scaled.jpg?w=940",
            potencia = "320 hp", aceleracion = "6.5 s", velocidadMaxima = "195 km/h", traccion = "RWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "4", brand = "Tesla", model = "Model 3", year = 2024, category = "Eléctrico",
            imageUrl = "https://cdn.motor1.com/images/mgl/eoOk4M/s1/prueba-tesla-model-3-traccion-trasera.jpg",
            potencia = "283 hp", aceleracion = "6.1 s", velocidadMaxima = "201 km/h", traccion = "RWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "5", brand = "Chevrolet", model = "Camaro SS", year = 2021, category = "Deportivo",
            imageUrl = "https://cdn-fastly.autoguide.com/media/2023/06/07/12309463/2016-chevrolet-camaro-ss-review-curbed-with-craig-cole.jpg?size=720x845&nocrop=1",
            potencia = "455 hp", aceleracion = "4.0 s", velocidadMaxima = "250 km/h", traccion = "RWD",
            ownerId = SEED_USER_RODRIGO
        ),
        VehicleFirestoreDto(
            id = "6", brand = "Jeep", model = "Wrangler", year = 2023, category = "SUV",
            imageUrl = "https://orangeclub.co/wp-content/uploads/2025/05/Jeep-Wrangler-Rubicon.jpg",
            potencia = "285 hp", aceleracion = "6.9 s", velocidadMaxima = "180 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "7", brand = "Volkswagen", model = "Beetle '65", year = 1965, category = "Clásico",
            imageUrl = "https://goauctionmpc.goauction.co.uk/stock/1708-0-medium.jpg?v=63829760351190",
            potencia = "50 hp", aceleracion = "27.0 s", velocidadMaxima = "125 km/h", traccion = "RWD",
            ownerId = SEED_USER_SOFIA
        ),
        // El 8 (Ford Ranger) se deja sin reseñas a propósito, para comprobar
        // que el detalle muestra calificación 0 y 0 reseñas.
        VehicleFirestoreDto(
            id = "8", brand = "Ford", model = "Ranger", year = 2022, category = "Pickup",
            imageUrl = "https://imagenes.portafolio.co/files/image_600_455/uploads/2021/01/29/6014dcacba87b.jpeg",
            potencia = "210 hp", aceleracion = "10.3 s", velocidadMaxima = "180 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        // 9-29: los mismos que agrega seed/seed.js para el feed personalizado.
        VehicleFirestoreDto(
            id = "9", brand = "Mazda", model = "MX-5", year = 2023, category = "Deportivo",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Mazda_Roadster_%28MX-5%29_by_Negawa_Bridge_%28cropped%29.jpg/1280px-Mazda_Roadster_%28MX-5%29_by_Negawa_Bridge_%28cropped%29.jpg",
            potencia = "181 hp", aceleracion = "6.5 s", velocidadMaxima = "219 km/h", traccion = "RWD",
            ownerId = SEED_USER_RODRIGO
        ),
        VehicleFirestoreDto(
            id = "10", brand = "BMW", model = "M2", year = 2024, category = "Deportivo",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/BMW_G87_M2_1X7A1838.jpg/1280px-BMW_G87_M2_1X7A1838.jpg",
            potencia = "453 hp", aceleracion = "4.1 s", velocidadMaxima = "250 km/h", traccion = "RWD",
            ownerId = SEED_USER_RODRIGO
        ),
        VehicleFirestoreDto(
            id = "11", brand = "Toyota", model = "GR86", year = 2023, category = "Deportivo",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/50/2022_Toyota_GR86_Premium_in_Halo%2C_Front_Right%2C_04-10-2022.jpg/1280px-2022_Toyota_GR86_Premium_in_Halo%2C_Front_Right%2C_04-10-2022.jpg",
            potencia = "228 hp", aceleracion = "6.3 s", velocidadMaxima = "226 km/h", traccion = "RWD",
            ownerId = SEED_USER_RODRIGO
        ),
        VehicleFirestoreDto(
            id = "12", brand = "Nissan", model = "Z", year = 2023, category = "Deportivo",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Nissan_FAIRLADY_Z_%28Z34%29_Version_ST%2C_2022%2C_left-front.jpg/1280px-Nissan_FAIRLADY_Z_%28Z34%29_Version_ST%2C_2022%2C_left-front.jpg",
            potencia = "400 hp", aceleracion = "4.5 s", velocidadMaxima = "250 km/h", traccion = "RWD",
            ownerId = SEED_USER_RODRIGO
        ),
        VehicleFirestoreDto(
            id = "13", brand = "Mazda", model = "CX-5", year = 2024, category = "SUV",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/2024_Mazda_CX-5_2.5_S_Select_in_Platinum_Quartz_Metallic%2C_front_right.jpg/1280px-2024_Mazda_CX-5_2.5_S_Select_in_Platinum_Quartz_Metallic%2C_front_right.jpg",
            potencia = "187 hp", aceleracion = "9.3 s", velocidadMaxima = "200 km/h", traccion = "AWD",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "14", brand = "Renault", model = "Duster", year = 2024, category = "SUV",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Dacia_Duster_TCe_130_Extreme_%28III%29_%E2%80%93_f_13102024.jpg/1280px-Dacia_Duster_TCe_130_Extreme_%28III%29_%E2%80%93_f_13102024.jpg",
            potencia = "130 hp", aceleracion = "11.2 s", velocidadMaxima = "185 km/h", traccion = "FWD",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "15", brand = "Kia", model = "Sportage", year = 2024, category = "SUV",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5d/2025_Kia_Sportage_S_front_only.jpg/1280px-2025_Kia_Sportage_S_front_only.jpg",
            potencia = "187 hp", aceleracion = "9.5 s", velocidadMaxima = "190 km/h", traccion = "AWD",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "16", brand = "Chevrolet", model = "Tracker", year = 2024, category = "SUV",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/39/2024_Chevrolet_Trax_2RS%2C_front_left%2C_12-10-2023.jpg/1280px-2024_Chevrolet_Trax_2RS%2C_front_left%2C_12-10-2023.jpg",
            potencia = "137 hp", aceleracion = "10.4 s", velocidadMaxima = "180 km/h", traccion = "FWD",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "17", brand = "Renault", model = "4", year = 1985, category = "Clásico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/1984_Renault_4_GTL%2C_front_left_%28Germany%29.jpg/1280px-1984_Renault_4_GTL%2C_front_left_%28Germany%29.jpg",
            potencia = "34 hp", aceleracion = "23.0 s", velocidadMaxima = "120 km/h", traccion = "FWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "18", brand = "Chevrolet", model = "Bel Air '57", year = 1957, category = "Clásico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2a/57_Chevy_BelAir.jpg",
            potencia = "220 hp", aceleracion = "9.5 s", velocidadMaxima = "175 km/h", traccion = "RWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "19", brand = "Mercedes-Benz", model = "280 SL", year = 1969, category = "Clásico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4e/1969_Mercedes_280_SL_Pagoda.jpg",
            potencia = "168 hp", aceleracion = "9.3 s", velocidadMaxima = "200 km/h", traccion = "RWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "20", brand = "Toyota", model = "Land Cruiser FJ40", year = 1982, category = "Clásico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/1982_Toyota_Land_Cruiser_FJ40_in_Freeborn_Red%2C_front_right.jpg/1280px-1982_Toyota_Land_Cruiser_FJ40_in_Freeborn_Red%2C_front_right.jpg",
            potencia = "135 hp", aceleracion = "16.0 s", velocidadMaxima = "135 km/h", traccion = "4x4",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "21", brand = "BYD", model = "Dolphin", year = 2024, category = "Eléctrico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/2021_BYD_Dolphin_EV_%28front%29.jpg/1280px-2021_BYD_Dolphin_EV_%28front%29.jpg",
            potencia = "201 hp", aceleracion = "7.0 s", velocidadMaxima = "160 km/h", traccion = "FWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "22", brand = "BYD", model = "Seal", year = 2024, category = "Eléctrico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/2022_BYD_Seal.jpg/1280px-2022_BYD_Seal.jpg",
            potencia = "308 hp", aceleracion = "5.9 s", velocidadMaxima = "180 km/h", traccion = "RWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "23", brand = "Hyundai", model = "Ioniq 5", year = 2024, category = "Eléctrico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/85/Hyundai_Ioniq_5_AWD_Techniq-Paket_%E2%80%93_f_31122024.jpg/1280px-Hyundai_Ioniq_5_AWD_Techniq-Paket_%E2%80%93_f_31122024.jpg",
            potencia = "320 hp", aceleracion = "5.1 s", velocidadMaxima = "185 km/h", traccion = "AWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "24", brand = "Kia", model = "EV6", year = 2024, category = "Eléctrico",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/2021_Kia_EV6_GT-Line_S.jpg/1280px-2021_Kia_EV6_GT-Line_S.jpg",
            potencia = "320 hp", aceleracion = "5.2 s", velocidadMaxima = "185 km/h", traccion = "AWD",
            ownerId = SEED_USER_SOFIA
        ),
        VehicleFirestoreDto(
            id = "25", brand = "Toyota", model = "Hilux", year = 2024, category = "Pickup",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/2016_Toyota_HiLux_Invincible_D-4D_4WD_2.4_Front.jpg/1280px-2016_Toyota_HiLux_Invincible_D-4D_4WD_2.4_Front.jpg",
            potencia = "201 hp", aceleracion = "10.8 s", velocidadMaxima = "175 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "26", brand = "Chevrolet", model = "Colorado", year = 2024, category = "Pickup",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fb/2024_Chevrolet_Colorado_Z71%2C_front_left%2C_09-28-2024.jpg/1280px-2024_Chevrolet_Colorado_Z71%2C_front_left%2C_09-28-2024.jpg",
            potencia = "310 hp", aceleracion = "7.0 s", velocidadMaxima = "180 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "27", brand = "Nissan", model = "Frontier", year = 2023, category = "Pickup",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/1/11/2021_Nissan_Navara_facelift_%28cropped%29.jpg",
            potencia = "190 hp", aceleracion = "10.5 s", velocidadMaxima = "180 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "28", brand = "Ford", model = "F-150", year = 2023, category = "Pickup",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f0/2018_Ford_F-150_XLT_Crew_Cab%2C_front_11.10.19.jpg/1280px-2018_Ford_F-150_XLT_Crew_Cab%2C_front_11.10.19.jpg",
            potencia = "400 hp", aceleracion = "6.1 s", velocidadMaxima = "180 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        ),
        VehicleFirestoreDto(
            id = "29", brand = "Mitsubishi", model = "L200", year = 2023, category = "Pickup",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Mitsubishi_Triton_LC_2.4_GLS_2WD_Blade_Silver_Metallic_%28cropped%29.jpg/1280px-Mitsubishi_Triton_LC_2.4_GLS_2WD_Blade_Silver_Metallic_%28cropped%29.jpg",
            potencia = "181 hp", aceleracion = "11.0 s", velocidadMaxima = "175 km/h", traccion = "4x4",
            ownerId = SEED_USER_IVAN
        )
    )

    /**
     * (autor, vehículo, rating, comentario, lat, lng), igual que
     * initReviews.js.
     *
     * Las coordenadas están repartidas por Bogotá con unos 1-3 km de
     * separación: así los marcadores se distinguen con el zoom de ciudad y
     * todos caben en pantalla. El seeder les pone la fecha escalonada, y solo
     * las primeras caen dentro de las 24 h que muestra el mapa.
     */
    val reviews = listOf(
        SeedReview(SEED_USER_RODRIGO, "1", 5, "El sonido del motor es otro nivel. Se siente pegado al piso en cada curva.", 4.6280, -74.0643, "Chapinero, Bogotá"),
        SeedReview(SEED_USER_SOFIA, "1", 5, "Impecable en pista, pero para ciudad es incómodo y muy duro.", 4.6560, -74.0550, "Usaquén, Bogotá"),
        SeedReview(SEED_USER_IVAN, "1", 4, "Una máquina, aunque el precio del mantenimiento asusta.", 4.6097, -74.0817, "La Candelaria, Bogotá"),
        SeedReview(SEED_USER_RODRIGO, "2", 4, "Lo saqué a trocha todo un fin de semana y no se quejó ni una vez.", 4.6760, -74.0480, "La Calera, Bogotá"),
        SeedReview(SEED_USER_IVAN, "2", 3, "Enorme y confiable, pero el consumo de gasolina es alto.", 4.7010, -74.0330, "Cerros Orientales, Bogotá"),
        SeedReview(SEED_USER_SOFIA, "3", 5, "Un clásico que voltea cabezas donde sea. Vale cada peso de la restauración.", 4.5980, -74.0760, "Teusaquillo, Bogotá"),
        SeedReview(SEED_USER_IVAN, "3", 4, "Hermoso, aunque sin frenos modernos hay que manejarlo con respeto.", 4.6440, -74.0900, "Barrios Unidos, Bogotá"),
        SeedReview(SEED_USER_RODRIGO, "4", 4, "El ahorro mensual se nota muchísimo. La autonomía me alcanza de sobra.", 4.6680, -74.1050, "Suba, Bogotá"),
        SeedReview(SEED_USER_SOFIA, "4", 3, "Buen carro, pero encontrar cargador rápido en carretera todavía cuesta.", 4.5810, -74.1180, "Kennedy, Bogotá"),
        SeedReview(SEED_USER_IVAN, "5", 4, "Potencia de sobra y buen precio para lo que entrega.", 4.7180, -74.0720, "Autopista Norte, Bogotá"),
        SeedReview(SEED_USER_RODRIGO, "6", 5, "Quitarle el techo un domingo no tiene comparación. Perfecto para montaña.", 4.6350, -74.0380, "Vía La Calera, Bogotá"),
        SeedReview(SEED_USER_SOFIA, "6", 3, "Divertidísimo fuera de la ciudad, pero en autopista suena mucho.", 4.5690, -74.0970, "Bosa, Bogotá"),
        SeedReview(SEED_USER_SOFIA, "7", 4, "Sencillo, económico y se arregla con cualquier mecánico. Un amor.", 4.6900, -74.0890, "Niza, Bogotá")
    )

    /**
     * placeName va quemado y no se geocodifica al sembrar: el seeder corre al
     * abrir la app por primera vez y no debería depender de que haya red ni
     * del servicio de geocodificación. Son las mismas coordenadas, con el
     * nombre que les corresponde.
     */
    data class SeedReview(
        val userId: String,
        val vehicleId: String,
        val rating: Int,
        val comment: String,
        val lat: Double,
        val lng: Double,
        val placeName: String
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
