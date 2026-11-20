/**
 * Siembra la base "motormatesdb" con datos ficticios generados con Faker.
 *
 * Mismas colecciones y mismos campos que leen los DTO de la app
 * (UserDto, VehicleFirestoreDto, ReviewFirestoreDto, LikeDto, FollowDto),
 * así que lo que se escribe acá se ve igual que lo que escribe la app.
 *
 * Uso:
 *   node seed.js                 -> escribe en el proyecto real
 *   node seed.js --parts         -> solo el catálogo de AI Tuning (piezas,
 *                                   paquetes y sus fotos en Storage)
 *   FIRESTORE_EMULATOR_HOST=localhost:8080 node seed.js
 *                                -> escribe en el emulador (el Admin SDK
 *                                   detecta esa variable por su cuenta)
 *
 * Los ids son fijos (seed_user_N, seed_review_N) y Faker va con semilla, así
 * que correrlo dos veces sobrescribe los mismos documentos en vez de
 * duplicarlos.
 */

const fs = require("fs");
const path = require("path");
const { initializeApp, applicationDefault } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getStorage, getDownloadURL } = require("firebase-admin/storage");
const { faker } = require("@faker-js/faker/locale/es");

const serviceAccount = require("./serviceAccountKey.json");

// La app apunta a la base con nombre "motormatesdb" (FirestoreModule.kt),
// no a la "(default)".
const DATABASE_ID = "motormatesdb";

// Un batch de Firestore acepta máximo 500 escrituras.
const BATCH_LIMIT = 500;

// Semilla fija: cada corrida genera exactamente los mismos datos.
faker.seed(42);

const USERS_COUNT = 15;
// Cada usuario reseña cada carro a lo sumo una vez, así que el tope real es
// USERS_COUNT. Con 4-7 por carro salen ~160 reseñas: suficientes para que el
// mapa forme clusters y para que el feed personalizado tenga variedad.
const MIN_REVIEWS_PER_VEHICLE = 4;
const MAX_REVIEWS_PER_VEHICLE = 7;

// Proporción de reseñas fechadas en las últimas 24 h (las que ve el mapa).
// El resto se reparte en el último mes para que el feed siga variado.
const RECENT_REVIEW_PROBABILITY = 0.8;

// Con cert(serviceAccount), firebase-admin le pasa las llaves a
// google-auth-library por la opción `credentials`, que está deprecada y
// suelta un aviso en consola. Apuntando GOOGLE_APPLICATION_CREDENTIALS al
// mismo archivo, la librería lo lee ella misma y no hay aviso. Si la
// variable ya viene del entorno, se respeta.
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  process.env.GOOGLE_APPLICATION_CREDENTIALS = path.join(__dirname, "serviceAccountKey.json");
}

initializeApp({
  credential: applicationDefault(),
  // Con credenciales por defecto el projectId no se infiere solo; se toma
  // del mismo archivo de llave.
  projectId: serviceAccount.project_id,
  // El bucket de app/google-services.json. Se puede cambiar con STORAGE_BUCKET.
  storageBucket: process.env.STORAGE_BUCKET || `${serviceAccount.project_id}.firebasestorage.app`,
});
const db = getFirestore(DATABASE_ID);

/**
 * Catálogo fijo, no generado: las fotos son URLs reales y los ids "1".."8"
 * son los mismos que usa el buscador de la app. Faker se encarga de lo que
 * sí es volumen (usuarios, reseñas, likes, seguidores).
 *
 * El 8 (Ford Ranger) se queda sin reseñas a propósito, para comprobar que el
 * detalle muestra calificación 0 y 0 reseñas. Los ids "9".."29" amplían el
 * catálogo para que el feed personalizado tenga de dónde escoger.
 */
const VEHICLES = [
  {
    id: "1", brand: "Porsche", model: "911 GT3", year: 2024, category: "Deportivo",
    imageUrl: "https://www.europeanprestige.co.uk/blobs/stock/577/images/45f3295c-923a-40fd-b1e7-6f6d0d129de2/hi4a4328.jpg?width=2000&height=1333",
    potencia: "502 hp", aceleracion: "3.4 s", velocidadMaxima: "318 km/h", traccion: "RWD",
  },
  {
    id: "2", brand: "Toyota", model: "Land Cruiser", year: 2022, category: "SUV",
    imageUrl: "https://autoamerica.com.co/wp-content/uploads/2020/08/4N4A2472.jpg",
    potencia: "409 hp", aceleracion: "6.7 s", velocidadMaxima: "210 km/h", traccion: "4x4",
  },
  {
    id: "3", brand: "Ford", model: "Mustang '67", year: 1967, category: "Clásico",
    imageUrl: "https://bringatrailer.com/wp-content/uploads/2026/07/1967_ford_mustang-gt_1967_ford_mustang-gt_445183c0-f15d-4fac-bfaf-aab6c2d8d11d-uhtvDu-46102-46103-scaled.jpg?w=940",
    potencia: "320 hp", aceleracion: "6.5 s", velocidadMaxima: "195 km/h", traccion: "RWD",
  },
  {
    id: "4", brand: "Tesla", model: "Model 3", year: 2024, category: "Eléctrico",
    imageUrl: "https://cdn.motor1.com/images/mgl/eoOk4M/s1/prueba-tesla-model-3-traccion-trasera.jpg",
    potencia: "283 hp", aceleracion: "6.1 s", velocidadMaxima: "201 km/h", traccion: "RWD",
  },
  {
    id: "5", brand: "Chevrolet", model: "Camaro SS", year: 2021, category: "Deportivo",
    imageUrl: "https://cdn-fastly.autoguide.com/media/2023/06/07/12309463/2016-chevrolet-camaro-ss-review-curbed-with-craig-cole.jpg?size=720x845&nocrop=1",
    potencia: "455 hp", aceleracion: "4.0 s", velocidadMaxima: "250 km/h", traccion: "RWD",
  },
  {
    id: "6", brand: "Jeep", model: "Wrangler", year: 2023, category: "SUV",
    imageUrl: "https://orangeclub.co/wp-content/uploads/2025/05/Jeep-Wrangler-Rubicon.jpg",
    potencia: "285 hp", aceleracion: "6.9 s", velocidadMaxima: "180 km/h", traccion: "4x4",
  },
  {
    id: "7", brand: "Volkswagen", model: "Beetle '65", year: 1965, category: "Clásico",
    imageUrl: "https://goauctionmpc.goauction.co.uk/stock/1708-0-medium.jpg?v=63829760351190",
    potencia: "50 hp", aceleracion: "27.0 s", velocidadMaxima: "125 km/h", traccion: "RWD",
  },
  {
    id: "8", brand: "Ford", model: "Ranger", year: 2022, category: "Pickup",
    imageUrl: "https://imagenes.portafolio.co/files/image_600_455/uploads/2021/01/29/6014dcacba87b.jpeg",
    potencia: "210 hp", aceleracion: "10.3 s", velocidadMaxima: "180 km/h", traccion: "4x4",
  },
  // 9-29: ampliación para que la personalización del feed (onboarding) se
  // note. Marcas comunes en Colombia, ~6 por categoría. Fotos de Wikimedia
  // Commons (licencias libres), en el tamaño de miniatura estándar de 1280 px.
  {
    id: "9", brand: "Mazda", model: "MX-5", year: 2023, category: "Deportivo",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Mazda_Roadster_%28MX-5%29_by_Negawa_Bridge_%28cropped%29.jpg/1280px-Mazda_Roadster_%28MX-5%29_by_Negawa_Bridge_%28cropped%29.jpg",
    potencia: "181 hp", aceleracion: "6.5 s", velocidadMaxima: "219 km/h", traccion: "RWD",
  },
  {
    id: "10", brand: "BMW", model: "M2", year: 2024, category: "Deportivo",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/BMW_G87_M2_1X7A1838.jpg/1280px-BMW_G87_M2_1X7A1838.jpg",
    potencia: "453 hp", aceleracion: "4.1 s", velocidadMaxima: "250 km/h", traccion: "RWD",
  },
  {
    id: "11", brand: "Toyota", model: "GR86", year: 2023, category: "Deportivo",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/5/50/2022_Toyota_GR86_Premium_in_Halo%2C_Front_Right%2C_04-10-2022.jpg/1280px-2022_Toyota_GR86_Premium_in_Halo%2C_Front_Right%2C_04-10-2022.jpg",
    potencia: "228 hp", aceleracion: "6.3 s", velocidadMaxima: "226 km/h", traccion: "RWD",
  },
  {
    id: "12", brand: "Nissan", model: "Z", year: 2023, category: "Deportivo",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Nissan_FAIRLADY_Z_%28Z34%29_Version_ST%2C_2022%2C_left-front.jpg/1280px-Nissan_FAIRLADY_Z_%28Z34%29_Version_ST%2C_2022%2C_left-front.jpg",
    potencia: "400 hp", aceleracion: "4.5 s", velocidadMaxima: "250 km/h", traccion: "RWD",
  },
  {
    id: "13", brand: "Mazda", model: "CX-5", year: 2024, category: "SUV",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/2024_Mazda_CX-5_2.5_S_Select_in_Platinum_Quartz_Metallic%2C_front_right.jpg/1280px-2024_Mazda_CX-5_2.5_S_Select_in_Platinum_Quartz_Metallic%2C_front_right.jpg",
    potencia: "187 hp", aceleracion: "9.3 s", velocidadMaxima: "200 km/h", traccion: "AWD",
  },
  {
    id: "14", brand: "Renault", model: "Duster", year: 2024, category: "SUV",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Dacia_Duster_TCe_130_Extreme_%28III%29_%E2%80%93_f_13102024.jpg/1280px-Dacia_Duster_TCe_130_Extreme_%28III%29_%E2%80%93_f_13102024.jpg",
    potencia: "130 hp", aceleracion: "11.2 s", velocidadMaxima: "185 km/h", traccion: "FWD",
  },
  {
    id: "15", brand: "Kia", model: "Sportage", year: 2024, category: "SUV",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5d/2025_Kia_Sportage_S_front_only.jpg/1280px-2025_Kia_Sportage_S_front_only.jpg",
    potencia: "187 hp", aceleracion: "9.5 s", velocidadMaxima: "190 km/h", traccion: "AWD",
  },
  {
    id: "16", brand: "Chevrolet", model: "Tracker", year: 2024, category: "SUV",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/3/39/2024_Chevrolet_Trax_2RS%2C_front_left%2C_12-10-2023.jpg/1280px-2024_Chevrolet_Trax_2RS%2C_front_left%2C_12-10-2023.jpg",
    potencia: "137 hp", aceleracion: "10.4 s", velocidadMaxima: "180 km/h", traccion: "FWD",
  },
  {
    id: "17", brand: "Renault", model: "4", year: 1985, category: "Clásico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/1984_Renault_4_GTL%2C_front_left_%28Germany%29.jpg/1280px-1984_Renault_4_GTL%2C_front_left_%28Germany%29.jpg",
    potencia: "34 hp", aceleracion: "23.0 s", velocidadMaxima: "120 km/h", traccion: "FWD",
  },
  {
    id: "18", brand: "Chevrolet", model: "Bel Air '57", year: 1957, category: "Clásico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/2/2a/57_Chevy_BelAir.jpg",
    potencia: "220 hp", aceleracion: "9.5 s", velocidadMaxima: "175 km/h", traccion: "RWD",
  },
  {
    id: "19", brand: "Mercedes-Benz", model: "280 SL", year: 1969, category: "Clásico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/4/4e/1969_Mercedes_280_SL_Pagoda.jpg",
    potencia: "168 hp", aceleracion: "9.3 s", velocidadMaxima: "200 km/h", traccion: "RWD",
  },
  {
    id: "20", brand: "Toyota", model: "Land Cruiser FJ40", year: 1982, category: "Clásico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/1982_Toyota_Land_Cruiser_FJ40_in_Freeborn_Red%2C_front_right.jpg/1280px-1982_Toyota_Land_Cruiser_FJ40_in_Freeborn_Red%2C_front_right.jpg",
    potencia: "135 hp", aceleracion: "16.0 s", velocidadMaxima: "135 km/h", traccion: "4x4",
  },
  {
    id: "21", brand: "BYD", model: "Dolphin", year: 2024, category: "Eléctrico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/2021_BYD_Dolphin_EV_%28front%29.jpg/1280px-2021_BYD_Dolphin_EV_%28front%29.jpg",
    potencia: "201 hp", aceleracion: "7.0 s", velocidadMaxima: "160 km/h", traccion: "FWD",
  },
  {
    id: "22", brand: "BYD", model: "Seal", year: 2024, category: "Eléctrico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/2022_BYD_Seal.jpg/1280px-2022_BYD_Seal.jpg",
    potencia: "308 hp", aceleracion: "5.9 s", velocidadMaxima: "180 km/h", traccion: "RWD",
  },
  {
    id: "23", brand: "Hyundai", model: "Ioniq 5", year: 2024, category: "Eléctrico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/8/85/Hyundai_Ioniq_5_AWD_Techniq-Paket_%E2%80%93_f_31122024.jpg/1280px-Hyundai_Ioniq_5_AWD_Techniq-Paket_%E2%80%93_f_31122024.jpg",
    potencia: "320 hp", aceleracion: "5.1 s", velocidadMaxima: "185 km/h", traccion: "AWD",
  },
  {
    id: "24", brand: "Kia", model: "EV6", year: 2024, category: "Eléctrico",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/2021_Kia_EV6_GT-Line_S.jpg/1280px-2021_Kia_EV6_GT-Line_S.jpg",
    potencia: "320 hp", aceleracion: "5.2 s", velocidadMaxima: "185 km/h", traccion: "AWD",
  },
  {
    id: "25", brand: "Toyota", model: "Hilux", year: 2024, category: "Pickup",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/2016_Toyota_HiLux_Invincible_D-4D_4WD_2.4_Front.jpg/1280px-2016_Toyota_HiLux_Invincible_D-4D_4WD_2.4_Front.jpg",
    potencia: "201 hp", aceleracion: "10.8 s", velocidadMaxima: "175 km/h", traccion: "4x4",
  },
  {
    id: "26", brand: "Chevrolet", model: "Colorado", year: 2024, category: "Pickup",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fb/2024_Chevrolet_Colorado_Z71%2C_front_left%2C_09-28-2024.jpg/1280px-2024_Chevrolet_Colorado_Z71%2C_front_left%2C_09-28-2024.jpg",
    potencia: "310 hp", aceleracion: "7.0 s", velocidadMaxima: "180 km/h", traccion: "4x4",
  },
  {
    id: "27", brand: "Nissan", model: "Frontier", year: 2023, category: "Pickup",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/1/11/2021_Nissan_Navara_facelift_%28cropped%29.jpg",
    potencia: "190 hp", aceleracion: "10.5 s", velocidadMaxima: "180 km/h", traccion: "4x4",
  },
  {
    id: "28", brand: "Ford", model: "F-150", year: 2023, category: "Pickup",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f0/2018_Ford_F-150_XLT_Crew_Cab%2C_front_11.10.19.jpg/1280px-2018_Ford_F-150_XLT_Crew_Cab%2C_front_11.10.19.jpg",
    potencia: "400 hp", aceleracion: "6.1 s", velocidadMaxima: "180 km/h", traccion: "4x4",
  },
  {
    id: "29", brand: "Mitsubishi", model: "L200", year: 2023, category: "Pickup",
    imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Mitsubishi_Triton_LC_2.4_GLS_2WD_Blade_Silver_Metallic_%28cropped%29.jpg/1280px-Mitsubishi_Triton_LC_2.4_GLS_2WD_Blade_Silver_Metallic_%28cropped%29.jpg",
    potencia: "181 hp", aceleracion: "11.0 s", velocidadMaxima: "175 km/h", traccion: "4x4",
  },
];

/** El Ranger se queda sin reseñas a propósito (ver comentario de VEHICLES). */
const VEHICLE_WITHOUT_REVIEWS = "8";

const CITIES = [
  "Bogotá", "Medellín", "Cali", "Barranquilla", "Bucaramanga",
  "Cartagena", "Pereira", "Manizales",
];

/**
 * Gustos de los usuarios semilla: categorías favoritas por posición. Sirven
 * para que cada uno reseñe sobre todo lo que le gusta (un fan de los SUV
 * reseña SUV) y así las sugerencias del onboarding tengan sentido.
 */
const TASTES = [
  ["Deportivo"], ["SUV"], ["Clásico"], ["Eléctrico"], ["Pickup"],
  ["Deportivo", "Clásico"], ["SUV", "Pickup"], ["Eléctrico", "Deportivo"],
  ["Clásico", "SUV"], ["Pickup", "SUV"], ["Deportivo"], ["Eléctrico", "SUV"],
  ["Clásico"], ["Deportivo", "Eléctrico"], ["SUV"],
];

/** Bio acorde con la primera categoría favorita. */
const BIO_BY_CATEGORY = {
  Deportivo: "Fan de los deportivos y los track days.",
  SUV: "Si tiene 4x4, lo saco a trocha.",
  Clásico: "Clásicos los domingos, restaurando el resto de la semana.",
  Eléctrico: "Eléctrico entre semana, silencioso y sin tanqueo.",
  Pickup: "Pickup para el trabajo y para el paseo de fin de semana.",
};

/** Probabilidad de que un autor reseñe en su propia ciudad. */
const OWN_CITY_REVIEW_PROBABILITY = 0.7;

// Faker no tiene frases de carros (faker.lorem sale en latín y se ve falso),
// así que las bios (BIO_BY_CATEGORY) y los comentarios se arman con pedazos
// propios.

const COMMENT_OPENERS = [
  "Lo manejé toda la semana y",
  "Después de 500 km en carretera",
  "Lo saqué un fin de semana completo y",
  "Llevo tres meses con él y",
  "Lo probé en ciudad y en montaña:",
  "Comparado con el que tenía antes,",
];

const COMMENT_PRAISE = [
  "responde como uno espera en cada curva",
  "el motor se siente de sobra para lo que uno necesita",
  "la posición de manejo es cómoda hasta en trancón",
  "el frenado da mucha confianza",
  "voltea cabezas donde sea",
  "no se queja ni una vez por más duro que lo trate",
];

const COMMENT_GRIPE = [
  "aunque el consumo de gasolina es alto",
  "pero el mantenimiento cuesta más de lo que uno calcula",
  "pero en autopista suena bastante",
  "aunque la suspensión es muy dura para estas calles",
  "pero el espacio atrás queda justo",
  "aunque encontrar repuestos toma su tiempo",
];

/**
 * Factoría de usuario: Faker rellena nombre y correo. La ciudad y los gustos
 * salen de la posición, así cada ciudad y categoría tienen sus usuarios.
 * preferences tiene la misma forma que guarda el onboarding de la app.
 */
function makeUser(index) {
  const fullName = faker.person.fullName();
  const username = faker.internet
    .username({ firstName: fullName.split(" ")[0] })
    .toLowerCase();
  const city = CITIES[index % CITIES.length];
  const categories = TASTES[index % TASTES.length];

  return {
    id: `seed_user_${index + 1}`,
    fullName,
    email: `${username}@motormates.com`,
    username,
    location: `${city}, Colombia`,
    bio: BIO_BY_CATEGORY[categories[0]],
    preferences: {
      categories,
      brands: [],
      interests: [],
      city,
      car: null,
      completed: true,
      skipped: false,
      updatedAt: Date.now(),
    },
    avatarUrl: null,
    coverUrl: null,
    followersCount: 0,
    followingCount: 0,
    createdAt: faker.date.past({ years: 2 }).getTime(),
  };
}

/**
 * Factoría de reseña, con el autor y el vehículo desnormalizados dentro.
 *
 * isRecent lo manda quien llama para la primera reseña de cada vehículo: esa
 * se fecha dentro de las últimas 24 h para que el mapa de reseñas recientes
 * nunca salga vacío después de sembrar. Las demás se reparten en el último
 * mes, así el feed sigue viéndose variado.
 */
function makeReview(index, author, vehicle, isRecent = false) {
  const rating = faker.number.int({ min: 3, max: 5 });
  // Una reseña de 5 no lleva queja; las de 3 y 4 sí.
  const gripe = rating === 5 ? "" : `, ${faker.helpers.arrayElement(COMMENT_GRIPE)}`;
  const comment = `${faker.helpers.arrayElement(COMMENT_OPENERS)} ` +
    `${faker.helpers.arrayElement(COMMENT_PRAISE)}${gripe}.`;
  const createdAt = isRecent
    ? Date.now() - faker.number.int({ min: 1, max: 20 }) * 60 * 60 * 1000
    : faker.date.recent({ days: 30 }).getTime();

  return {
    id: `seed_review_${index + 1}`,
    rating,
    comment,
    userId: author.id,
    vehicleId: vehicle.id,
    createdAt,
    updatedAt: createdAt,
    likesCount: 0, // se ajusta al repartir los likes
    user: { fullName: author.fullName, avatarUrl: author.avatarUrl },
    vehicle: { brand: vehicle.brand, model: vehicle.model },
    // Dónde se publicó, para el mapa de reseñas recientes.
    location: randomReviewLocation(index, author.preferences.city),
  };
}

/**
 * Zonas donde se concentran las reseñas. Puntos al azar por toda la ciudad
 * casi nunca se agrupan; concentrados alrededor de sitios conocidos, al
 * alejar el zoom se juntan en clusters y al acercar se separan. weight = qué
 * tan seguido sale cada zona.
 *
 * El nombre va fijo en vez de geocodificar: el seeder no debería depender de
 * una API externa, y para datos de demostración basta con que sea verosímil.
 */
const REVIEW_HOTSPOTS = [
  { lat: 4.6766, lng: -74.0482, placeName: "Parque 93, Bogotá", weight: 4 },
  { lat: 4.6667, lng: -74.0530, placeName: "Zona T, Bogotá", weight: 4 },
  { lat: 4.6950, lng: -74.0300, placeName: "Usaquén, Bogotá", weight: 3 },
  { lat: 4.7020, lng: -74.0410, placeName: "Unicentro, Bogotá", weight: 2 },
  { lat: 4.5981, lng: -74.0760, placeName: "La Candelaria, Bogotá", weight: 2 },
  { lat: 4.6510, lng: -74.1100, placeName: "Salitre, Bogotá", weight: 2 },
  // Otras ciudades: al alejar del todo se ven como grupos aparte.
  { lat: 6.2088, lng: -75.5670, placeName: "El Poblado, Medellín", weight: 2 },
  { lat: 3.4570, lng: -76.5340, placeName: "Granada, Cali", weight: 1 },
];

/**
 * Un sitio por ciudad para las reseñas que el autor hace en su propia
 * ciudad: así "Popular en tu ciudad" del feed tiene datos en todas.
 */
const CITY_SPOTS = {
  Bogotá: { lat: 4.6766, lng: -74.0482, placeName: "Parque 93, Bogotá" },
  Medellín: { lat: 6.2088, lng: -75.5670, placeName: "El Poblado, Medellín" },
  Cali: { lat: 3.4570, lng: -76.5340, placeName: "Granada, Cali" },
  Barranquilla: { lat: 11.0012, lng: -74.8113, placeName: "Alto Prado, Barranquilla" },
  Bucaramanga: { lat: 7.1193, lng: -73.1227, placeName: "Cabecera, Bucaramanga" },
  Cartagena: { lat: 10.4236, lng: -75.5485, placeName: "Bocagrande, Cartagena" },
  Pereira: { lat: 4.8133, lng: -75.6961, placeName: "Circunvalar, Pereira" },
  Manizales: { lat: 5.0567, lng: -75.4840, placeName: "El Cable, Manizales" },
};

/**
 * Un punto exacto que se repite (como varias reseñas desde el mismo centro
 * comercial). Ningún zoom lo separa: sirve para probar la lista del cluster.
 */
const SAME_SPOT = { lat: 4.6677, lng: -74.0526, placeName: "Centro Comercial Andino, Bogotá" };
// Fijo y no al azar: así siempre hay varias en ese punto, aunque cambie la semilla.
const SAME_SPOT_EVERY = 9;

/** ~0.003 grados son unos 300 m: lo bastante cerca para agruparse, lejos para separarse al acercar. */
const HOTSPOT_JITTER_DEGREES = 0.003;

function randomReviewLocation(index, authorCity) {
  if (index % SAME_SPOT_EVERY === SAME_SPOT_EVERY / 3) {
    return { ...SAME_SPOT };
  }

  const ownCity = CITY_SPOTS[authorCity] &&
    faker.datatype.boolean({ probability: OWN_CITY_REVIEW_PROBABILITY });
  const hotspot = ownCity
    ? CITY_SPOTS[authorCity]
    : faker.helpers.weightedArrayElement(
      REVIEW_HOTSPOTS.map((spot) => ({ weight: spot.weight, value: spot }))
    );
  // Suma de dos uniformes: los puntos se cargan hacia el centro de la zona
  // en vez de llenar un cuadrado parejo.
  const jitter = () =>
    (faker.number.float({ min: -1, max: 1 }) + faker.number.float({ min: -1, max: 1 })) / 2 *
    HOTSPOT_JITTER_DEGREES;

  return {
    lat: Number((hotspot.lat + jitter()).toFixed(5)),
    lng: Number((hotspot.lng + jitter()).toFixed(5)),
    placeName: hotspot.placeName,
  };
}

/**
 * Arma todo en memoria antes de escribir: así los contadores
 * (likesCount, followersCount, followingCount) salen cuadrados con los
 * documentos de las subcolecciones, sin necesidad de transacciones.
 */
/**
 * Car Meets y piques de ejemplo.
 *
 * Se siembran a propósito con variedad: uno de cada tipo, uno cancelado, uno
 * en curso y dos en el mismo punto exacto. Así la capa de eventos del mapa se
 * puede comprobar entera (colores por tipo, estilo de cancelado, la hoja del
 * grupo que ningún zoom separa) sin tener que crearlos a mano.
 *
 * Los campos son los de EventFirestoreDto: userId/user como en las reseñas, y
 * el tipo y las características como texto, no como enum.
 */
function buildEvents(users) {
  const hora = 60 * 60 * 1000;
  const dia = 24 * hora;
  const ahora = Date.now();

  const plantillas = [
    {
      id: "seed_event_1",
      type: "CAR_MEET",
      title: "Encuentro de clásicos en el Parque 93",
      description: "Traigan sus clásicos. Hay zona de fotos y food trucks.",
      venueName: "Parque 93",
      address: "Calle 93 #13-50, Bogotá",
      location: { lat: 4.6766, lng: -74.0482, placeName: "Parque 93, Bogotá" },
      startAt: ahora + 2 * dia + 19 * hora,
      endAt: ahora + 2 * dia + 23 * hora,
      status: "SCHEDULED",
      features: ["EXHIBICION", "ZONA_FOTOS", "FOOD_TRUCKS", "ENTRADA_LIBRE"],
    },
    {
      id: "seed_event_2",
      type: "PIQUE",
      title: "Pique cronometrado en Tocancipá",
      description: "Pista cerrada. Inscripción previa obligatoria.",
      venueName: "Autódromo de Tocancipá",
      address: "Km 35 Autopista Norte",
      location: { lat: 4.9667, lng: -73.9122, placeName: "Tocancipá, Cundinamarca" },
      startAt: ahora + 5 * dia + 9 * hora,
      endAt: ahora + 5 * dia + 17 * hora,
      status: "SCHEDULED",
      features: [
        "CRONOMETRAJE",
        "PISTA_CERRADA",
        "CASCO_OBLIGATORIO",
        "EXTINTOR_ABORDO",
        "AMBULANCIA_EN_SITIO",
        "INSCRIPCION_PREVIA",
      ],
    },
    {
      // En curso: empezó hace un rato y aún no termina, así que el mapa lo
      // tiene que seguir mostrando (ver la ventana de la capa de eventos).
      id: "seed_event_3",
      type: "CAR_MEET",
      title: "Café y carros en Usaquén",
      description: "Nos tomamos algo y miramos carros, sin más.",
      venueName: "Usaquén",
      address: "Carrera 6 #119-30, Bogotá",
      location: { lat: 4.695, lng: -74.03, placeName: "Usaquén, Bogotá" },
      startAt: ahora - hora,
      endAt: ahora + 2 * hora,
      status: "SCHEDULED",
      features: ["APTO_FAMILIAS", "PARQUEADERO"],
    },
    {
      // Cancelado: sigue visible para que quien dijo que iba se entere.
      id: "seed_event_4",
      type: "CAR_MEET",
      title: "Exhibición en Salitre (cancelada)",
      description: "Se cancela por el clima. Reprogramamos pronto.",
      venueName: "Salitre",
      address: "Avenida 68, Bogotá",
      location: { lat: 4.651, lng: -74.11, placeName: "Salitre, Bogotá" },
      startAt: ahora + 3 * dia + 15 * hora,
      endAt: null,
      status: "CANCELLED",
      features: ["EXHIBICION", "PREMIACION"],
    },
    {
      // Los dos siguientes comparten el punto EXACTO del centro comercial,
      // igual que SAME_SPOT en las reseñas: ningún zoom los separa, así que
      // sirven para probar la hoja del grupo de eventos.
      id: "seed_event_5",
      type: "CAR_MEET",
      title: "Meet matutino en el Andino",
      description: null,
      venueName: "Centro Comercial Andino",
      address: "Carrera 11 #82-71, Bogotá",
      location: { ...SAME_SPOT },
      startAt: ahora + dia + 8 * hora,
      endAt: null,
      status: "SCHEDULED",
      features: ["PARQUEADERO"],
    },
    {
      id: "seed_event_6",
      type: "CAR_MEET",
      title: "Meet nocturno en el Andino",
      description: null,
      venueName: "Centro Comercial Andino",
      address: "Carrera 11 #82-71, Bogotá",
      location: { ...SAME_SPOT },
      startAt: ahora + dia + 20 * hora,
      endAt: ahora + dia + 23 * hora,
      status: "SCHEDULED",
      features: ["MUSICA", "PARQUEADERO"],
    },
  ];

  return plantillas.map((plantilla, index) => {
    const organizador = users[index % users.length];
    return {
      ...plantilla,
      userId: organizador.id,
      // Copia desnormalizada, igual que en las reseñas.
      user: { fullName: organizador.fullName, avatarUrl: organizador.avatarUrl },
      interestedCount: 0,
      goingCount: 0,
      createdAt: ahora,
      updatedAt: ahora,
    };
  });
}

function buildData() {
  const users = Array.from({ length: USERS_COUNT }, (_, i) => makeUser(i));

  const reviews = [];
  // El Ranger se deja sin reseñas (ver comentario de VEHICLES).
  VEHICLES.filter((vehicle) => vehicle.id !== VEHICLE_WITHOUT_REVIEWS).forEach((vehicle) => {
    // Primero los fans de la categoría y luego el resto, sin repetir: nadie
    // reseña dos veces el mismo carro y cada uno reseña sobre todo lo suyo.
    const count = faker.number.int({ min: MIN_REVIEWS_PER_VEHICLE, max: MAX_REVIEWS_PER_VEHICLE });
    const fans = faker.helpers.shuffle(
      users.filter((user) => user.preferences.categories.includes(vehicle.category))
    );
    const others = faker.helpers.shuffle(
      users.filter((user) => !user.preferences.categories.includes(vehicle.category))
    );
    const authors = [...fans.slice(0, count - 1), ...others].slice(0, count);
    // La primera de cada vehículo siempre es reciente, para que el mapa nunca
    // salga vacío; de las demás, la mayoría también.
    authors.forEach((author, i) => {
      const isRecent = i === 0 || faker.datatype.boolean({ probability: RECENT_REVIEW_PROBABILITY });
      reviews.push(makeReview(reviews.length, author, vehicle, isRecent));
    });
  });

  // Likes: cualquiera menos el propio autor.
  const likes = [];
  reviews.forEach((review) => {
    const candidates = users.filter((user) => user.id !== review.userId);
    const likers = faker.helpers.arrayElements(candidates, { min: 0, max: 5 });
    likers.forEach((liker) => {
      likes.push({
        reviewId: review.id,
        userId: liker.id,
        createdAt: faker.date.recent({ days: 15 }).getTime(),
      });
    });
    review.likesCount = likers.length;
  });

  // Seguidores: cada usuario sigue a unos cuantos, menos a sí mismo.
  const follows = [];
  users.forEach((user) => {
    const targets = faker.helpers.arrayElements(
      users.filter((other) => other.id !== user.id),
      { min: 1, max: 4 }
    );
    targets.forEach((target) => {
      follows.push({
        followerId: user.id,
        targetId: target.id,
        createdAt: faker.date.recent({ days: 60 }).getTime(),
      });
    });
    user.followingCount = targets.length;
  });
  follows.forEach(({ targetId }) => {
    const target = users.find((user) => user.id === targetId);
    target.followersCount += 1;
  });

  const events = buildEvents(users);

  return { users, reviews, likes, follows, events };
}

/** Escribe en lotes de 500: cada batch es atómico por sí solo. */
async function commitInBatches(writes) {
  for (let start = 0; start < writes.length; start += BATCH_LIMIT) {
    const batch = db.batch();
    writes.slice(start, start + BATCH_LIMIT).forEach(({ ref, data }) => batch.set(ref, data));
    await batch.commit();
  }
}

/**
 * Catálogo de AI Tuning. La fuente es el mismo archivo que la app trae en
 * assets (así nunca se desincronizan): app/src/main/assets/parts_catalog.json.
 *
 * Fotos de las piezas: pon cada una en seed/parts/images/<id>.(png|jpg|jpeg|webp)
 * (por ejemplo rin_18_5radios_negro.jpg) y este script la sube a Storage en
 * parts/<id>.<ext> y le pone su imageUrl. Las piezas sin foto se muestran con
 * un ícono y la AI las dibuja solo a partir de su descripción.
 */
const CATALOG_PATH = path.join(__dirname, "..", "app", "src", "main", "assets", "parts_catalog.json");
const PART_IMAGES_DIR = path.join(__dirname, "parts", "images");
const IMAGE_EXTENSIONS = [".png", ".jpg", ".jpeg", ".webp"];
const CONTENT_TYPES = { ".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".webp": "image/webp" };

function findPartImage(partId) {
  for (const ext of IMAGE_EXTENSIONS) {
    const file = path.join(PART_IMAGES_DIR, partId + ext);
    if (fs.existsSync(file)) return { file, ext };
  }
  return null;
}

async function uploadPartImage(partId) {
  const found = findPartImage(partId);
  if (!found) return null;
  // Con el emulador de Firestore activo no hay emulador de Storage en este
  // proyecto: las fotos se suben igual al bucket real.
  const destination = `parts/${partId}${found.ext}`;
  const bucket = getStorage().bucket();
  await bucket.upload(found.file, {
    destination,
    metadata: { contentType: CONTENT_TYPES[found.ext] },
  });
  return getDownloadURL(bucket.file(destination));
}

async function seedParts() {
  const catalog = JSON.parse(fs.readFileSync(CATALOG_PATH, "utf8"));
  const writes = [];
  let withImage = 0;

  for (const { id, ...data } of catalog.parts) {
    // Una foto local (seed/parts/images) gana sobre la URL del catálogo.
    const imageUrl = await uploadPartImage(id);
    if (imageUrl) data.imageUrl = imageUrl;
    if (data.imageUrl) withImage += 1;
    writes.push({ ref: db.collection("parts").doc(id), data });
  }
  catalog.packages.forEach(({ id, ...data }) => {
    writes.push({ ref: db.collection("tuningPackages").doc(id), data });
  });

  await commitInBatches(writes);
  console.log(
    `Catálogo AI Tuning: ${catalog.parts.length} piezas (${withImage} con foto) ` +
    `y ${catalog.packages.length} paquetes.`
  );
}

async function main() {
  if (process.argv.includes("--parts")) {
    await seedParts();
    return;
  }

  const { users, reviews, likes, follows, events } = buildData();
  const writes = [];

  // El id va en la ruta del documento, no dentro de él: en la app lo
  // rellena @DocumentId al deserializar.
  VEHICLES.forEach(({ id, ...data }) => {
    writes.push({ ref: db.collection("vehicles").doc(id), data });
  });

  users.forEach(({ id, ...data }) => {
    writes.push({ ref: db.collection("users").doc(id), data });
  });

  reviews.forEach(({ id, ...data }) => {
    writes.push({ ref: db.collection("reviews").doc(id), data });
  });

  events.forEach(({ id, ...data }) => {
    writes.push({ ref: db.collection("events").doc(id), data });
  });

  likes.forEach(({ reviewId, userId, createdAt }) => {
    writes.push({
      ref: db.collection("reviews").doc(reviewId).collection("likes").doc(userId),
      data: { userId, createdAt },
    });
  });

  follows.forEach(({ followerId, targetId, createdAt }) => {
    // Las dos caras del seguimiento, igual que FirestoreFollowDataSource.
    writes.push({
      ref: db.collection("users").doc(targetId).collection("followers").doc(followerId),
      data: { createdAt },
    });
    writes.push({
      ref: db.collection("users").doc(followerId).collection("following").doc(targetId),
      data: { createdAt },
    });
  });

  await commitInBatches(writes);
  await seedParts();

  const destino = process.env.FIRESTORE_EMULATOR_HOST
    ? `emulador (${process.env.FIRESTORE_EMULATOR_HOST})`
    : `proyecto ${serviceAccount.project_id}`;
  console.log(
    `Listo en ${destino}: ${VEHICLES.length} vehículos, ${users.length} usuarios, ` +
    `${reviews.length} reseñas, ${events.length} eventos, ${likes.length} likes ` +
    `y ${follows.length} seguimientos.`
  );
}

main().catch((error) => {
  console.error("El seed falló:", error);
  process.exitCode = 1;
});
