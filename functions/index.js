const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

// La app usa la base de datos con nombre "motormatesdb" (FirestoreModule.kt),
// no la "(default)": el trigger y el cliente admin deben apuntar a ella o la
// función nunca se dispara.
const DATABASE_ID = "motormatesdb";

initializeApp();
const db = getFirestore(DATABASE_ID);

// Un batch acepta máximo 500 escrituras.
const BATCH_LIMIT = 500;

/**
 * Integridad de datos desnormalizados.
 *
 * Cada reseña guarda una copia del autor (user.fullName y user.avatarUrl)
 * para pintarse sin leer el perfil. Cuando el usuario cambia su nombre o
 * su foto en users/{userId}, esta función actualiza esa copia en todas sus
 * reseñas, así las viejas no se quedan con los datos anteriores.
 */
exports.updateUserInfoInReviews = onDocumentUpdated(
  { document: "users/{userId}", database: DATABASE_ID },
  async (event) => {
    const userId = event.params.userId;
    const before = event.data.before.data();
    const after = event.data.after.data();

    // Optimización: el perfil también cambia por contadores de seguidores,
    // bio, portada, etc. Si ni el nombre ni la foto cambiaron, no hay nada
    // que propagar.
    const nameChanged = before.fullName !== after.fullName;
    const avatarChanged = (before.avatarUrl ?? null) !== (after.avatarUrl ?? null);
    if (!nameChanged && !avatarChanged) {
      return;
    }

    const reviewsSnapshot = await db.collection("reviews")
      .where("userId", "==", userId)
      .get();

    if (reviewsSnapshot.empty) {
      logger.log(`El usuario ${userId} no tiene reseñas publicadas.`);
      return;
    }

    const changes = {};
    if (nameChanged) changes["user.fullName"] = after.fullName ?? "";
    if (avatarChanged) changes["user.avatarUrl"] = after.avatarUrl ?? null;

    // Se reparte en lotes de 500: cada batch es atómico por sí solo.
    const docs = reviewsSnapshot.docs;
    for (let start = 0; start < docs.length; start += BATCH_LIMIT) {
      const batch = db.batch();
      docs.slice(start, start + BATCH_LIMIT).forEach((doc) => batch.update(doc.ref, changes));
      await batch.commit();
    }

    logger.log(`Datos del autor actualizados en ${docs.length} reseñas del usuario ${userId}.`);
  }
);
