/**
 * Siembra la base "motormatesdb" con datos ficticios generados con Faker.
 *
 * Mismas colecciones y mismos campos que leen los DTO de la app
 * (UserDto, VehicleFirestoreDto, ReviewFirestoreDto, LikeDto, FollowDto),
 * así que lo que se escribe acá se ve igual que lo que escribe la app.
 *
 * Uso:
 *   node seed.js                 -> escribe en el proyecto real
 *   FIRESTORE_EMULATOR_HOST=localhost:8080 node seed.js
 *                                -> escribe en el emulador (el Admin SDK
 *                                   detecta esa variable por su cuenta)
 *
 * Los ids son fijos (seed_user_N, seed_review_N) y Faker va con semilla, así
 * que correrlo dos veces sobrescribe los mismos documentos en vez de
 * duplicarlos.
 */

const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { faker } = require("@faker-js/faker/locale/es");

const serviceAccount = require("./serviceAccountKey.json");

// La app apunta a la base con nombre "motormatesdb" (FirestoreModule.kt),
// no a la "(default)".
const DATABASE_ID = "motormatesdb";

// Un batch de Firestore acepta máximo 500 escrituras.
const BATCH_LIMIT = 500;

// Semilla fija: cada corrida genera exactamente los mismos datos.
faker.seed(42);

const USERS_COUNT = 8;
const MIN_REVIEWS_PER_VEHICLE = 1;
const MAX_REVIEWS_PER_VEHICLE = 4;

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore(DATABASE_ID);

/**
 * Catálogo fijo, no generado: las fotos son URLs reales y los ids "1".."8"
 * son los mismos que usa el buscador de la app. Faker se encarga de lo que
 * sí es volumen (usuarios, reseñas, likes, seguidores).
 *
 * El 8 (Ford Ranger) se queda sin reseñas a propósito, para comprobar que el
 * detalle muestra calificación 0 y 0 reseñas.
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
];

const CITIES = [
  "Bogotá", "Medellín", "Cali", "Barranquilla", "Bucaramanga",
  "Cartagena", "Pereira", "Manizales",
];

// Faker no tiene frases de carros (faker.lorem sale en latín y se ve falso),
// así que las bios y los comentarios se arman combinando pedazos propios.
const BIO_PARTS = [
  "Fan de los deportivos y los track days.",
  "Clásicos los domingos, eléctrico entre semana.",
  "Si tiene 4x4, lo saco a trocha.",
  "Mecánico de garaje los fines de semana.",
  "Colecciono motores viejos y ruidosos.",
  "Más kilómetros que fotos en la galería.",
  "Me gusta más el camino que el destino.",
  "Primero la suspensión, después el rin.",
];

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

/** Factoría de usuario: Faker rellena nombre, correo y ciudad. */
function makeUser(index) {
  const fullName = faker.person.fullName();
  const username = faker.internet
    .username({ firstName: fullName.split(" ")[0] })
    .toLowerCase();

  return {
    id: `seed_user_${index + 1}`,
    fullName,
    email: `${username}@motormates.com`,
    username,
    location: `${faker.helpers.arrayElement(CITIES)}, Colombia`,
    bio: faker.helpers.arrayElement(BIO_PARTS),
    avatarUrl: null,
    coverUrl: null,
    followersCount: 0,
    followingCount: 0,
    createdAt: faker.date.past({ years: 2 }).getTime(),
  };
}

/** Factoría de reseña, con el autor y el vehículo desnormalizados dentro. */
function makeReview(index, author, vehicle) {
  const rating = faker.number.int({ min: 3, max: 5 });
  // Una reseña de 5 no lleva queja; las de 3 y 4 sí.
  const gripe = rating === 5 ? "" : `, ${faker.helpers.arrayElement(COMMENT_GRIPE)}`;
  const comment = `${faker.helpers.arrayElement(COMMENT_OPENERS)} ` +
    `${faker.helpers.arrayElement(COMMENT_PRAISE)}${gripe}.`;
  const createdAt = faker.date.recent({ days: 30 }).getTime();

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
  };
}

/**
 * Arma todo en memoria antes de escribir: así los contadores
 * (likesCount, followersCount, followingCount) salen cuadrados con los
 * documentos de las subcolecciones, sin necesidad de transacciones.
 */
function buildData() {
  const users = Array.from({ length: USERS_COUNT }, (_, i) => makeUser(i));

  const reviews = [];
  // El último vehículo se deja sin reseñas (ver comentario de VEHICLES).
  VEHICLES.slice(0, -1).forEach((vehicle) => {
    // arrayElements no repite, así que nadie reseña dos veces el mismo carro.
    const authors = faker.helpers.arrayElements(users, {
      min: MIN_REVIEWS_PER_VEHICLE,
      max: MAX_REVIEWS_PER_VEHICLE,
    });
    authors.forEach((author) => reviews.push(makeReview(reviews.length, author, vehicle)));
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

  return { users, reviews, likes, follows };
}

/** Escribe en lotes de 500: cada batch es atómico por sí solo. */
async function commitInBatches(writes) {
  for (let start = 0; start < writes.length; start += BATCH_LIMIT) {
    const batch = db.batch();
    writes.slice(start, start + BATCH_LIMIT).forEach(({ ref, data }) => batch.set(ref, data));
    await batch.commit();
  }
}

async function main() {
  const { users, reviews, likes, follows } = buildData();
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

  const destino = process.env.FIRESTORE_EMULATOR_HOST
    ? `emulador (${process.env.FIRESTORE_EMULATOR_HOST})`
    : `proyecto ${serviceAccount.project_id}`;
  console.log(
    `Listo en ${destino}: ${VEHICLES.length} vehículos, ${users.length} usuarios, ` +
    `${reviews.length} reseñas, ${likes.length} likes y ${follows.length} seguimientos.`
  );
}

main().catch((error) => {
  console.error("El seed falló:", error);
  process.exitCode = 1;
});
