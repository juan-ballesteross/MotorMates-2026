package com.example.motormates.data.repository

import com.example.motormates.data.source.LocalCarCatalogDataSource
import com.example.motormates.data.source.RemoteCarCatalogDataSource
import com.example.motormates.data.util.normalizedKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Un modelo para el autocompletado, con de dónde salió. */
data class CarModelOption(val name: String, val source: CarModelSource)

enum class CarModelSource { LOCAL, VPIC }

/**
 * Marcas y modelos para "¿Cuál es tu carro?" del onboarding.
 *
 * Combina dos fuentes: la lista local del mercado colombiano (siempre está,
 * incluso sin red) y vPIC de la NHTSA (API real, más completa para las
 * marcas que se venden en EE. UU.). Si vPIC no responde a tiempo, quedan los
 * modelos locales y el usuario siempre puede escribir el suyo.
 */
interface CarCatalogRepository {
    suspend fun brands(): List<String>
    suspend fun models(brand: String, year: Int): List<CarModelOption>
}

@Singleton
class CarCatalogRepositoryImpl @Inject constructor(
    private val local: LocalCarCatalogDataSource,
    private val remote: RemoteCarCatalogDataSource
) : CarCatalogRepository {

    private val mutex = Mutex()
    private var localCache: Map<String, List<String>>? = null
    private val remoteCache = mutableMapOf<String, List<String>>()

    override suspend fun brands(): List<String> = localCatalog().keys.sortedBy { it.normalizedKey() }

    override suspend fun models(brand: String, year: Int): List<CarModelOption> {
        val key = brand.normalizedKey()
        val localEntry = localCatalog().entries.firstOrNull { it.key.normalizedKey() == key }
        val localModels = localEntry?.value.orEmpty()
        // A vPIC se le pregunta con el nombre bien escrito si lo conocemos.
        val remoteModels = remoteModels(localEntry?.key ?: brand.trim(), year)

        // Los locales primero (son los que se ven en Colombia); de vPIC solo
        // los que no estén ya, comparando sin tildes ni mayúsculas.
        val seen = localModels.map { it.normalizedKey() }.toMutableSet()
        val extra = remoteModels.filter { seen.add(it.normalizedKey()) }.sortedBy { it.normalizedKey() }
        return localModels.map { CarModelOption(it, CarModelSource.LOCAL) } +
            extra.map { CarModelOption(it, CarModelSource.VPIC) }
    }

    private suspend fun localCatalog(): Map<String, List<String>> = mutex.withLock {
        localCache ?: runCatching { local.load() }.getOrDefault(emptyMap()).also { localCache = it }
    }

    private suspend fun remoteModels(brand: String, year: Int): List<String> {
        val cacheKey = "${brand.normalizedKey()}|$year"
        mutex.withLock { remoteCache[cacheKey] }?.let { return it }

        val models = try {
            withTimeoutOrNull(REMOTE_TIMEOUT_MS) { remote.models(brand.trim(), year) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        // Solo se guarda una respuesta real: si falló, se reintenta la próxima vez.
        models?.let { mutex.withLock { remoteCache[cacheKey] = it } }
        return models.orEmpty()
    }

    companion object {
        const val REMOTE_TIMEOUT_MS = 6_000L
    }
}
