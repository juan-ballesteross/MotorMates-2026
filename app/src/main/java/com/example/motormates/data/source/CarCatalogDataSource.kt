package com.example.motormates.data.source

import android.content.Context
import com.example.motormates.data.remote.service.VpicApiService
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** Marcas y modelos del mercado colombiano, empaquetados en la app. */
interface LocalCarCatalogDataSource {
    /** marca -> modelos, en el orden del archivo. */
    suspend fun load(): Map<String, List<String>>
}

/** Modelos de una marca y año según vPIC (NHTSA). */
interface RemoteCarCatalogDataSource {
    suspend fun models(make: String, year: Int): List<String>
}

class AssetCarCatalogDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : LocalCarCatalogDataSource {

    override suspend fun load(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        val json = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        CarCatalogParser.parse(json, gson)
    }

    private companion object {
        const val ASSET_FILE = "car_models_co.json"
    }
}

object CarCatalogParser {
    private data class CatalogJson(val brands: List<BrandJson>? = null)
    private data class BrandJson(val name: String? = null, val models: List<String>? = null)

    fun parse(json: String, gson: Gson = Gson()): Map<String, List<String>> =
        gson.fromJson(json, CatalogJson::class.java).brands.orEmpty()
            .mapNotNull { brand ->
                val name = brand.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                name to brand.models.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            }
            .toMap()
}

class VpicCarCatalogDataSource @Inject constructor(
    private val api: VpicApiService
) : RemoteCarCatalogDataSource {

    /**
     * Tres consultas en paralelo: sin filtrar por tipo, vPIC mezcla motos y
     * buses (Suzuki devuelve motos, BYD buses). Si una falla, las demás
     * igual sirven; si fallan todas, se lanza la primera excepción.
     */
    override suspend fun models(make: String, year: Int): List<String> = coroutineScope {
        val results = VEHICLE_TYPES.map { type ->
            async { runCatching { api.getModels(make, year, type) } }
        }.awaitAll()

        if (results.all { it.isFailure }) throw results.first().exceptionOrNull()!!

        results.mapNotNull { it.getOrNull() }
            .flatMap { response -> response.results.mapNotNull { it.modelName?.trim() } }
            .filter { it.isNotEmpty() }
    }

    private companion object {
        val VEHICLE_TYPES = listOf("car", "mpv", "truck")
    }
}
