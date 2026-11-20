package com.example.motormates.data.repository

import com.example.motormates.data.model.PartsCatalog
import com.example.motormates.data.source.LocalPartsCatalogDataSource
import com.example.motormates.data.source.RemotePartsCatalogDataSource
import javax.inject.Inject
import javax.inject.Singleton

interface PartsCatalogRepository {
    /** Catálogo completo; se filtra por vehículo con [PartsCatalog.compatibleWith]. */
    suspend fun getCatalog(): Result<PartsCatalog>
}

/**
 * Firestore primero (lo que subió el seeder, con fotos en Storage). Si la
 * colección está vacía o no hay red, se usa el catálogo empaquetado en la
 * app, que es el mismo archivo: la demo funciona igual.
 *
 * Se guarda en memoria: el catálogo no cambia mientras la app está abierta.
 */
@Singleton
class PartsCatalogRepositoryImpl @Inject constructor(
    private val remote: RemotePartsCatalogDataSource,
    private val local: LocalPartsCatalogDataSource
) : PartsCatalogRepository {

    @Volatile
    private var cached: PartsCatalog? = null

    override suspend fun getCatalog(): Result<PartsCatalog> {
        cached?.let { return Result.success(it) }

        val remoteCatalog = runCatching { remote.getCatalog() }.getOrNull()
        val catalog = if (remoteCatalog != null && remoteCatalog.parts.isNotEmpty()) {
            remoteCatalog
        } else {
            runCatching { local.getCatalog() }.getOrElse {
                return Result.failure(Exception("No se pudo cargar el catálogo de piezas"))
            }
        }
        if (catalog.parts.isEmpty()) {
            return Result.failure(Exception("El catálogo de piezas está vacío"))
        }
        cached = catalog
        return Result.success(catalog)
    }
}
