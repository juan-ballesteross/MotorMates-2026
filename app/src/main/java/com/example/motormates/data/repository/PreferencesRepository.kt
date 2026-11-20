package com.example.motormates.data.repository

import com.example.motormates.data.dto.PreferencesDto
import com.example.motormates.data.model.UserPreferences
import com.example.motormates.data.source.AuthDataSource
import com.example.motormates.data.source.PreferencesDataSource
import com.example.motormates.data.util.TimeSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface PreferencesRepository {
    /** Las del usuario actual; vacías (sin decidir) si nunca respondió. */
    suspend fun getMine(): Result<UserPreferences>

    /** Las del usuario actual, en vivo: el feed se reordena al editarlas. */
    fun observeMine(): Flow<UserPreferences>

    /** Guarda y marca el onboarding como terminado. */
    suspend fun save(preferences: UserPreferences): Result<Unit>

    /** "Ahora no": conserva lo que ya hubiera y no vuelve a preguntar solo. */
    suspend fun skip(): Result<Unit>
}

class PreferencesRepositoryImpl @Inject constructor(
    private val dataSource: PreferencesDataSource,
    private val authDataSource: AuthDataSource,
    private val timeSource: TimeSource
) : PreferencesRepository {

    override suspend fun getMine(): Result<UserPreferences> {
        val uid = authDataSource.getCurrentUser()?.id ?: return Result.failure(Exception(NO_SESSION))
        return safeCall(LOAD_FAILED) { dataSource.get(uid)?.toDomain() ?: UserPreferences() }
    }

    override fun observeMine(): Flow<UserPreferences> {
        val uid = authDataSource.getCurrentUser()?.id ?: return flowOf(UserPreferences())
        // Sin red o sin permiso el feed sigue funcionando, solo que sin personalizar.
        return dataSource.observe(uid)
            .map { it?.toDomain() ?: UserPreferences() }
            .catch { emit(UserPreferences()) }
    }

    override suspend fun save(preferences: UserPreferences): Result<Unit> {
        val uid = authDataSource.getCurrentUser()?.id ?: return Result.failure(Exception(NO_SESSION))
        val toSave = preferences.copy(completed = true, skipped = false, updatedAt = timeSource.nowMillis())
        return safeCall(SAVE_FAILED) { dataSource.save(uid, PreferencesDto.fromDomain(toSave)) }
    }

    override suspend fun skip(): Result<Unit> {
        val uid = authDataSource.getCurrentUser()?.id ?: return Result.failure(Exception(NO_SESSION))
        return safeCall(SAVE_FAILED) {
            val current = dataSource.get(uid) ?: PreferencesDto()
            dataSource.save(uid, current.copy(skipped = true, updatedAt = timeSource.nowMillis()))
        }
    }

    private suspend fun <T> safeCall(fallback: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseNetworkException) {
        Result.failure(Exception(NO_NETWORK, e))
    } catch (e: FirebaseFirestoreException) {
        val message = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) NO_NETWORK else fallback
        Result.failure(Exception(message, e))
    } catch (e: Exception) {
        Result.failure(Exception(fallback, e))
    }

    companion object {
        const val NO_SESSION = "Debes iniciar sesión"
        const val NO_NETWORK = "Sin conexión a internet, verifica tu red"
        const val LOAD_FAILED = "No se pudieron cargar tus preferencias"
        const val SAVE_FAILED = "No se pudieron guardar tus preferencias"
    }
}
