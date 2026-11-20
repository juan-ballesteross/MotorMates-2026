package com.example.motormates.data.repository

import com.example.motormates.data.dto.BuildFirestoreDto
import com.example.motormates.data.dto.InsightsDto
import com.example.motormates.data.dto.SpecsDto
import com.example.motormates.data.model.Build
import com.example.motormates.data.model.BuildDraft
import com.example.motormates.data.model.TuningImage
import com.example.motormates.data.source.BuildDataSource
import com.example.motormates.data.util.TimeSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

interface BuildRepository {
    /** Sube la imagen y guarda el Build. publish = true lo muestra a la comunidad. */
    suspend fun save(draft: BuildDraft, image: TuningImage, publish: Boolean): Result<Build>

    suspend fun getMyBuilds(): Result<List<Build>>

    suspend fun getPublishedForVehicle(vehicleId: String): Result<List<Build>>

    /** Builds publicados de otro usuario (para su perfil público). */
    suspend fun getPublishedByOwner(uid: String): Result<List<Build>>

    suspend fun getBuild(buildId: String): Result<Build>

    suspend fun setPublished(buildId: String, published: Boolean): Result<Unit>

    suspend fun delete(buildId: String): Result<Unit>
}

class BuildRepositoryImpl @Inject constructor(
    private val dataSource: BuildDataSource,
    private val authRepository: AuthRepository,
    private val timeSource: TimeSource
) : BuildRepository {

    override suspend fun save(draft: BuildDraft, image: TuningImage, publish: Boolean): Result<Build> =
        runCatchingBuild("No se pudo guardar el build") {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("Debes iniciar sesión para guardar builds")
            val buildId = dataSource.newBuildId()
            val imageUrl = dataSource.uploadImage(user.id, buildId, image)
            val summary = draft.summary
            val dto = BuildFirestoreDto(
                id = buildId,
                vehicleId = draft.vehicleId,
                vehicleName = draft.vehicleName,
                ownerId = user.id,
                ownerName = user.displayName,
                baseImageUrl = draft.baseImageUrl,
                resultImageUrl = imageUrl,
                partIds = draft.parts.map { it.id },
                partNames = draft.parts.map { it.name },
                mode = draft.mode.name,
                prompt = draft.prompt,
                specsBefore = SpecsDto.from(summary.before),
                specsAfter = SpecsDto.from(summary.after),
                hpDeltaPct = summary.hpDeltaPct,
                torqueDeltaPct = summary.torqueDeltaPct,
                weightDeltaKg = summary.weightDeltaKg,
                heightDeltaMm = summary.heightDeltaMm,
                totalMinCop = summary.totalMinCop,
                totalMaxCop = summary.totalMaxCop,
                hasEstimatedPrices = summary.hasEstimatedPrices,
                insights = draft.insights?.let { InsightsDto.from(it) },
                published = publish,
                createdAt = timeSource.nowMillis()
            )
            dataSource.save(dto)
            dto.toDomain()
        }

    override suspend fun getMyBuilds(): Result<List<Build>> =
        runCatchingBuild("No se pudieron cargar tus builds") {
            val uid = authRepository.getCurrentUser()?.id
                ?: throw IllegalStateException("Debes iniciar sesión")
            dataSource.getByOwner(uid).map { it.toDomain() }
        }

    override suspend fun getPublishedForVehicle(vehicleId: String): Result<List<Build>> =
        runCatchingBuild("No se pudieron cargar los builds de la comunidad") {
            dataSource.getPublishedByVehicle(vehicleId).map { it.toDomain() }
        }

    override suspend fun getPublishedByOwner(uid: String): Result<List<Build>> =
        runCatchingBuild("No se pudieron cargar los builds") {
            dataSource.getPublishedByOwner(uid).map { it.toDomain() }
        }

    override suspend fun getBuild(buildId: String): Result<Build> =
        runCatchingBuild("No se pudo cargar el build") {
            dataSource.getById(buildId)?.toDomain()
                ?: throw NoSuchElementException("Este build no existe o fue eliminado")
        }

    override suspend fun setPublished(buildId: String, published: Boolean): Result<Unit> =
        runCatchingBuild("No se pudo actualizar el build") {
            dataSource.setPublished(buildId, published)
        }

    override suspend fun delete(buildId: String): Result<Unit> =
        runCatchingBuild("No se pudo eliminar el build") {
            val uid = authRepository.getCurrentUser()?.id
                ?: throw IllegalStateException("Debes iniciar sesión")
            dataSource.delete(uid, buildId)
        }

    private inline fun <T> runCatchingBuild(fallback: String, block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IllegalStateException) {
        Result.failure(Exception(e.message ?: fallback))
    } catch (e: NoSuchElementException) {
        Result.failure(Exception(e.message ?: fallback))
    } catch (e: FirebaseNetworkException) {
        Result.failure(Exception("Sin conexión a internet, verifica tu red"))
    } catch (e: FirebaseFirestoreException) {
        Result.failure(Exception(e.toSpanishMessage(notFound = "Este build no existe", fallback = fallback)))
    } catch (e: StorageException) {
        Result.failure(Exception("No se pudo subir la imagen del build"))
    } catch (e: Exception) {
        Result.failure(Exception(fallback))
    }
}
