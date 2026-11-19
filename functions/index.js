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
 * Cada reseña y cada evento guardan una copia del autor (user.fullName y
 * user.avatarUrl) para pintarse sin leer el perfil. Cuando el usuario cambia
 * su nombre o su foto en users/{userId}, esta función actualiza esa copia en
 * todos sus documentos, así los viejos no se quedan con los datos anteriores.
 *
 * Las dos colecciones se recorren en el MISMO trigger y no en funciones
 * separadas: dos triggers sobre users/{userId} duplicarían las invocaciones.
 * Los nombres de campo (userId, user.fullName, user.avatarUrl) son iguales en
 * "reviews" y en "events" justamente para poder compartir este código.
 *
 * El nombre se conserva aunque ya cubra más que las reseñas: renombrar una
 * función desplegada equivale a borrarla y crear otra.
 */
const DENORMALIZED_USER_COLLECTIONS = ["reviews", "events"];
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

    const changes = {};
    if (nameChanged) changes["user.fullName"] = after.fullName ?? "";
    if (avatarChanged) changes["user.avatarUrl"] = after.avatarUrl ?? null;

    for (const collection of DENORMALIZED_USER_COLLECTIONS) {
      const snapshot = await db.collection(collection)
        .where("userId", "==", userId)
        .get();

      if (snapshot.empty) {
        logger.log(`El usuario ${userId} no tiene documentos en ${collection}.`);
        continue;
      }

      // Se reparte en lotes de 500: cada batch es atómico por sí solo.
      const docs = snapshot.docs;
      for (let start = 0; start < docs.length; start += BATCH_LIMIT) {
        const batch = db.batch();
        docs.slice(start, start + BATCH_LIMIT).forEach((doc) => batch.update(doc.ref, changes));
        await batch.commit();
      }

      logger.log(
        `Datos del autor actualizados en ${docs.length} documentos de ${collection} del usuario ${userId}.`
      );
    }
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
 * Notificación push cuando alguien marca interés o asistencia en tu evento.
 *
 * onDocumentCreated y no onDocumentWritten, y por eso no hace falta comparar
 * before/after: el set de la transacción de participación
 * (FirestoreEventParticipationDataSource) CREA el documento al marcar por
 * primera vez y lo SOBREESCRIBE al pasar de "me interesa" a "voy". Así este
 * trigger se dispara exactamente en los dos casos que hay que avisar, y no al
 * cambiar de estado ni al quitar la participación.
 *
 * Un trigger por acción y no uno genérico sobre
 * users/{uid}/notifications/{id}: el genérico daría push también a likes,
 * reseñas y mensajes, y el de mensajes dispararía uno por CADA mensaje de una
 * conversación. Además el documento de la campana lo escribe la app en
 * best-effort, mientras que el de la acción es la verdad transaccional.
 * Cuando se decida darle push a lo demás, este trigger y
 * sendFollowNotification se funden en uno.
 *
 * Sin `retry`: en v2 está desactivado por defecto y la semántica es "a lo
 * sumo una vez". Un reintento mandaría un segundo push y la función no
 * escribe nada en Firestore, así que no habría forma de hacerla idempotente
 * sin un documento centinela. Perder un push suelto es aceptable; duplicarlo
 * en el celular de alguien, no.
 */
exports.sendEventParticipationNotification = onDocumentCreated(
  { document: "events/{eventId}/participants/{participantUid}", database: DATABASE_ID },
  async (event) => {
    const { eventId, participantUid } = event.params;

    // El estado viene en el documento que acabó de crearse: no hace falta
    // volver a leerlo.
    const status = event.data?.data()?.status;
    if (status !== "INTERESTED" && status !== "GOING") {
      return;
    }

    const eventSnapshot = await db.collection("events").doc(eventId).get();
    if (!eventSnapshot.exists) {
      return;
    }

    // El organizador se guarda en userId, no en organizerId: el nombre de
    // campo es el mismo que en "reviews" para que updateUserInfoInReviews
    // pueda recorrer las dos colecciones con las mismas rutas (ver
    // EventFirestoreDto). organizerId solo existe en el dominio de la app.
    const organizerId = eventSnapshot.data().userId;

    // Nadie se notifica a sí mismo (mismo criterio que la app).
    if (!organizerId || organizerId === participantUid) {
      return;
    }

    const [participantSnapshot, tokensSnapshot] = await Promise.all([
      db.collection("users").doc(participantUid).get(),
      db.collection("users").doc(organizerId).collection("fcmTokens").get(),
    ]);

    if (tokensSnapshot.empty) {
      logger.log(`El usuario ${organizerId} no tiene dispositivos registrados.`);
      return;
    }

    const participantName = participantSnapshot.data()?.fullName || "Alguien";
    const eventTitle = eventSnapshot.data().title || "tu evento";
    const tokens = tokensSnapshot.docs.map((doc) => doc.id);
    const going = status === "GOING";

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: going ? "Alguien va a tu evento" : "Le interesa tu evento",
        body: going
          ? `${participantName} va a ir a ${eventTitle}`
          : `A ${participantName} le interesa ${eventTitle}`,
      },
      // Los valores de type son idénticos a las constantes de NotificationType
      // en Kotlin, igual que "FOLLOW".
      data: {
        type: going ? "EVENT_GOING" : "EVENT_INTERESTED",
        actorId: participantUid,
        eventId,
      },
    });

    logger.log(
      `Push de participación para ${organizerId}: ${response.successCount} enviados, ${response.failureCount} fallidos.`
    );

    await deleteStaleTokens(organizerId, tokens, response.responses);
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
