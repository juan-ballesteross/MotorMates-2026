package com.example.motormates.data.repository

import com.example.motormates.data.model.NotificationType
import com.example.motormates.data.model.User
import com.example.motormates.data.source.FollowDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

interface FollowRepository {
    suspend fun followUser(followerUid: String, targetUid: String): Result<Unit>
    suspend fun unfollowUser(followerUid: String, targetUid: String): Result<Unit>
    suspend fun isFollowing(followerUid: String, targetUid: String): Result<Boolean>
    fun observeFollowingIds(uid: String): Flow<List<String>>
    fun observeFollowers(uid: String): Flow<Result<List<User>>>
    fun observeFollowing(uid: String): Flow<Result<List<User>>>
}

class FollowRepositoryImpl @Inject constructor(
    private val followDataSource: FollowDataSource,
    private val notificationRepository: NotificationRepository
) : FollowRepository {

    /**
     * Si el follow se guarda, se le avisa a la persona seguida. La regla
     * vive aquí y no en el ViewModel: seguir a alguien siempre notifica,
     * venga de la pantalla que venga. Dejar de seguir no notifica.
     */
    override suspend fun followUser(followerUid: String, targetUid: String): Result<Unit> {
        val result = safeFollowCall { followDataSource.followUser(followerUid, targetUid) }
        if (result.isSuccess) {
            notificationRepository.notify(
                recipientUid = targetUid,
                type = NotificationType.FOLLOW,
                actorId = followerUid
            )
        }
        return result
    }

    override suspend fun unfollowUser(followerUid: String, targetUid: String): Result<Unit> =
        safeFollowCall { followDataSource.unfollowUser(followerUid, targetUid) }

    /**
     * Atrapa los errores de las dos fuentes posibles (Firestore y el backend
     * propio), así cambiar el FollowDataSource inyectado no cambia los mensajes.
     */
    private suspend fun safeFollowCall(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(Exception(e.toFollowMessage()))
        } catch (e: IOException) {
            Result.failure(Exception(NO_CONNECTION_MESSAGE))
        } catch (e: HttpException) {
            Result.failure(
                Exception(
                    e.toSpanishMessage(
                        notFound = "Este usuario ya no existe",
                        fallback = "No se pudo completar la acción, intenta de nuevo"
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Ocurrió un error inesperado"))
        }
    }

    override suspend fun isFollowing(followerUid: String, targetUid: String): Result<Boolean> {
        return try {
            Result.success(followDataSource.isFollowing(followerUid, targetUid))
        } catch (e: Exception) {
            // No bloquea la pantalla: si falla, el botón arranca en "Seguir".
            Result.success(false)
        }
    }

    /** Mismo criterio que isFollowing: si falla, el feed se ve vacío pero no roto. */
    override fun observeFollowingIds(uid: String): Flow<List<String>> =
        followDataSource.observeFollowingIds(uid).catch { emit(emptyList()) }

    override fun observeFollowers(uid: String): Flow<Result<List<User>>> =
        followDataSource.observeFollowers(uid)
            .map { users -> Result.success(users.map { it.toDomain() }) }
            .catch { emit(Result.failure(Exception("No se pudo cargar la lista de seguidores"))) }

    override fun observeFollowing(uid: String): Flow<Result<List<User>>> =
        followDataSource.observeFollowing(uid)
            .map { users -> Result.success(users.map { it.toDomain() }) }
            .catch { emit(Result.failure(Exception("No se pudo cargar la lista de seguidos"))) }

    private fun FirebaseFirestoreException.toFollowMessage(): String = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sin conexión a internet, verifica tu red"
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para hacer esto"
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "Este usuario ya no existe"
        else -> "No se pudo completar la acción, intenta de nuevo"
    }
}
