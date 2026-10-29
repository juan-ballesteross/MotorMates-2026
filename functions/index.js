const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

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

/**
 * Notificación push cuando alguien te sigue.
 *
 * La app, al seguir a alguien, escribe el documento
 * users/{userId}/followers/{followerId} (FirestoreFollowDataSource) y además
 * deja la notificación en la campana. El push se manda desde acá y no desde
 * la app porque el dispositivo del destinatario puede estar cerrado: el
 * único que sabe a qué tokens enviar es el servidor.
 */
exports.sendFollowNotification = onDocumentCreated(
  { document: "users/{userId}/followers/{followerId}", database: DATABASE_ID },
  async (event) => {
    const { userId, followerId } = event.params;

    // Nadie se notifica a sí mismo (mismo criterio que la app).
    if (userId === followerId) {
      return;
    }

    const [followerSnapshot, tokensSnapshot] = await Promise.all([
      db.collection("users").doc(followerId).get(),
      db.collection("users").doc(userId).collection("fcmTokens").get(),
    ]);

    if (tokensSnapshot.empty) {
      logger.log(`El usuario ${userId} no tiene dispositivos registrados.`);
      return;
    }

    const followerName = followerSnapshot.data()?.fullName || "Alguien";
    const tokens = tokensSnapshot.docs.map((doc) => doc.id);

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "Tienes un nuevo seguidor",
        body: `${followerName} empezó a seguirte`,
      },
      // Datos para que la app sepa de qué se trata sin volver a consultar.
      data: { type: "FOLLOW", actorId: followerId },
    });

    logger.log(
      `Push de seguimiento para ${userId}: ${response.successCount} enviados, ${response.failureCount} fallidos.`
    );

    await deleteStaleTokens(userId, tokens, response.responses);
  }
);

/**
 * Un token deja de servir cuando el usuario desinstala la app o borra sus
 * datos, y FCM lo reporta con este código. Si no se limpian, la lista crece
 * y cada envío arrastra tokens muertos.
 */
async function deleteStaleTokens(userId, tokens, responses) {
  const staleTokens = responses
    .map((result, index) => ({ result, token: tokens[index] }))
    .filter(({ result }) =>
      result.error?.code === "messaging/registration-token-not-registered")
    .map(({ token }) => token);

  if (staleTokens.length === 0) {
    return;
  }

  const tokensCollection = db.collection("users").doc(userId).collection("fcmTokens");
  for (let start = 0; start < staleTokens.length; start += BATCH_LIMIT) {
    const batch = db.batch();
    staleTokens
      .slice(start, start + BATCH_LIMIT)
      .forEach((token) => batch.delete(tokensCollection.doc(token)));
    await batch.commit();
  }

  logger.log(`Se borraron ${staleTokens.length} tokens inválidos del usuario ${userId}.`);
}
