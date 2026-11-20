package com.example.motormates.data.source

import com.example.motormates.data.dto.BuildFirestoreDto
import com.example.motormates.data.model.TuningImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Builds guardados: la imagen generada va a Storage
 * (builds/{uid}/{buildId}.jpg) y el documento a la colección "builds".
 */
interface BuildDataSource {
    /** Reserva un id nuevo, sin escribir nada. */
    fun newBuildId(): String

    /** Sube la imagen y devuelve su URL de descarga. */
    suspend fun uploadImage(uid: String, buildId: String, image: TuningImage): String

    suspend fun save(build: BuildFirestoreDto)

    suspend fun getByOwner(uid: String): List<BuildFirestoreDto>

    suspend fun getPublishedByVehicle(vehicleId: String): List<BuildFirestoreDto>

    suspend fun getPublishedByOwner(uid: String): List<BuildFirestoreDto>

    suspend fun getById(buildId: String): BuildFirestoreDto?

    suspend fun setPublished(buildId: String, published: Boolean)

    suspend fun delete(uid: String, buildId: String)
}

class FirestoreBuildDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : BuildDataSource {

    private val builds get() = firestore.collection(BUILDS_COLLECTION)

    override fun newBuildId(): String = builds.document().id

    override suspend fun uploadImage(uid: String, buildId: String, image: TuningImage): String {
        val ref = storage.reference.child("builds/$uid/$buildId.jpg")
        val metadata = storageMetadata { contentType = image.mimeType() }
        ref.putBytes(image.bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun save(build: BuildFirestoreDto) {
        // El id va en la ruta, no dentro del documento (@DocumentId).
        builds.document(build.id).set(build.toFirestoreMap()).await()
    }

    // Las consultas solo usan igualdades (no necesitan índice compuesto);
    // el orden por fecha se hace en el cliente.
    override suspend fun getByOwner(uid: String): List<BuildFirestoreDto> =
        builds.whereEqualTo("ownerId", uid).get().await()
            .toObjects(BuildFirestoreDto::class.java)
            .sortedByDescending { it.createdAt }

    override suspend fun getPublishedByVehicle(vehicleId: String): List<BuildFirestoreDto> =
        builds.whereEqualTo("vehicleId", vehicleId)
            .whereEqualTo("published", true)
            .get().await()
            .toObjects(BuildFirestoreDto::class.java)
            .sortedByDescending { it.createdAt }

    override suspend fun getPublishedByOwner(uid: String): List<BuildFirestoreDto> =
        builds.whereEqualTo("ownerId", uid)
            .whereEqualTo("published", true)
            .get().await()
            .toObjects(BuildFirestoreDto::class.java)
            .sortedByDescending { it.createdAt }

    override suspend fun getById(buildId: String): BuildFirestoreDto? =
        builds.document(buildId).get().await().toObject(BuildFirestoreDto::class.java)

    override suspend fun setPublished(buildId: String, published: Boolean) {
        builds.document(buildId).update("published", published).await()
    }

    override suspend fun delete(uid: String, buildId: String) {
        builds.document(buildId).delete().await()
        // La imagen es best-effort: si ya no existe, el build igual se borró.
        runCatching { storage.reference.child("builds/$uid/$buildId.jpg").delete().await() }
    }
}

/** Sin el id: Firestore lo guarda en la ruta del documento. */
private fun BuildFirestoreDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    "vehicleId" to vehicleId,
    "vehicleName" to vehicleName,
    "ownerId" to ownerId,
    "ownerName" to ownerName,
    "baseImageUrl" to baseImageUrl,
    "resultImageUrl" to resultImageUrl,
    "partIds" to partIds,
    "partNames" to partNames,
    "mode" to mode,
    "prompt" to prompt,
    "specsBefore" to specsBefore,
    "specsAfter" to specsAfter,
    "hpDeltaPct" to hpDeltaPct,
    "torqueDeltaPct" to torqueDeltaPct,
    "weightDeltaKg" to weightDeltaKg,
    "heightDeltaMm" to heightDeltaMm,
    "totalMinCop" to totalMinCop,
    "totalMaxCop" to totalMaxCop,
    "hasEstimatedPrices" to hasEstimatedPrices,
    "insights" to insights,
    "published" to published,
    "createdAt" to createdAt
)

/** PNG empieza con 0x89 'P' 'N' 'G'; lo demás se trata como JPEG. */
internal fun TuningImage.mimeType(): String =
    if (bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
    ) "image/png" else "image/jpeg"
